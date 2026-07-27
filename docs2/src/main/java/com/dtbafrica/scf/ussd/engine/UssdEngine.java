package com.dtbafrica.scf.ussd.engine;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.domain.ProfileSnapshot;
import com.dtbafrica.scf.ussd.menu.MenuNode;
import com.dtbafrica.scf.ussd.menu.MenuRegistry;
import com.dtbafrica.scf.ussd.menu.NodeId;
import com.dtbafrica.scf.ussd.menu.Transition;
import com.dtbafrica.scf.ussd.menu.UssdContext;
import com.dtbafrica.scf.ussd.session.SessionState;
import com.dtbafrica.scf.ussd.session.SessionStore;
import org.springframework.stereotype.Service;

/**
 * Drives the menu tree. Maps the three Infobip calls onto node transitions.
 */
@Service
public class UssdEngine {

    private final MenuRegistry registry;
    private final SessionStore sessions;
    private final ProfileClient profile;

    public UssdEngine(MenuRegistry registry, SessionStore sessions, ProfileClient profile) {
        this.registry = registry;
        this.sessions = sessions;
        this.profile = profile;
    }

    /**
     * POST /session/{id}/start — the one Profile lookup, then pick the entry screen.
     */
    public UssdResponse start(String sessionId, String msisdn) {
        SessionState state = new SessionState(msisdn);
        try {
            ProfileSnapshot snapshot = profile.resolveByMsisdn(msisdn);
            state.setProfile(snapshot);
            state.setCurrent(entryNodeFor(snapshot));
        } catch (RuntimeException e) {
            state.setCurrent(NodeId.SERVICE_UNAVAILABLE);
        }
        return renderCurrent(sessionId, state);
    }

    /** PUT /session/{id}/response */
    public UssdResponse respond(String sessionId, String rawInput) {
        SessionState state = sessions.load(sessionId)
                .orElseThrow(() -> new UnknownSessionException(sessionId));
        UssdContext ctx = new UssdContext(sessionId, state);
        MenuNode node = registry.get(state.getCurrent());

        String input = rawInput == null ? "" : rawInput.trim();
        try {
            apply(node.next(input, ctx), state);
        } catch (RuntimeException e) {
            // Any downstream failure lands on one screen rather than a 500 to the gateway.
            state.setCurrent(NodeId.SERVICE_UNAVAILABLE);
        }
        return renderCurrent(sessionId, state);
    }

    /**
     * PUT /session/{id}/end — idempotent, because end arrives on timeouts and aborts too.
     */
    public void end(String sessionId, int exitCode, String reason) {
        sessions.delete(sessionId);
    }

    /**
     * Entry routing. The two pending states exist because registration spans two
     * sessions; without them a redial would re-issue an OTP forever.
     */
    private NodeId entryNodeFor(ProfileSnapshot snapshot) {
        return switch (snapshot.status()) {
            case NOT_A_SUPPLIER, NO_DTB_ACCOUNT, ELIGIBLE -> NodeId.LANGUAGE;
            case OTP_PENDING, PIN_RESET_PENDING -> NodeId.OTP_ENTRY;
            case REGISTERED -> NodeId.PIN_LOGIN;
        };
    }

    private void apply(Transition transition, SessionState state) {
        switch (transition) {
            case Transition.Goto g -> {
                state.backStack().push(state.getCurrent());
                state.setCurrent(g.node());
            }
            case Transition.Replace r -> state.setCurrent(r.node());
            case Transition.Back b -> state.setCurrent(
                    state.backStack().isEmpty() ? rootFor(state) : state.backStack().pop());
            case Transition.Root r -> {
                state.backStack().clear();
                state.setCurrent(rootFor(state));
            }
        }
    }

    /**
     * There is still no main menu in the source material. Anchor select is the de facto
     * root for a registered user; onboarding returns to the entry screen.
     */
    private NodeId rootFor(SessionState state) {
        ProfileSnapshot snapshot = state.getProfile();
        boolean registered = snapshot != null
                && (snapshot.status() == com.dtbafrica.scf.ussd.domain.RegistrationStatus.REGISTERED
                    || state.isJustRegistered());
        return registered ? NodeId.ANCHOR_SELECT : NodeId.LANGUAGE;
    }

    private UssdResponse renderCurrent(String sessionId, SessionState state) {
        MenuNode node = registry.get(state.getCurrent());
        String menu = node.render(new UssdContext(sessionId, state));
        if (node.terminal()) {
            sessions.delete(sessionId);
        } else {
            sessions.save(sessionId, state);
        }
        return UssdResponse.ok(node.terminal(), menu);
    }
}

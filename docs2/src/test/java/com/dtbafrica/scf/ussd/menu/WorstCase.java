package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.domain.*;
import com.dtbafrica.scf.ussd.session.SessionState;

import java.math.BigDecimal;
import java.util.List;

/**
 * Worst-case rendering inputs. Rendering with "Kamau" and a 5-figure limit proves
 * nothing about a supplier called Wanjiku-Kamau borrowing against an anchor with a long
 * name, so every interpolated value here is at or beyond the plausible maximum.
 */
public final class WorstCase {

    private WorstCase() {}

    static ProfileSnapshot profile() {
        return new ProfileSnapshot(
                "SUP-000000001",
                "Wanjiku-Kamau Njoroge",          // longer than NAME_MAX, must be clipped
                RegistrationStatus.REGISTERED,
                "en");
    }

    static List<Anchor> anchors() {
        return List.of(
                new Anchor("A1", "Kabianga Tea Factory Limited"),
                new Anchor("A2", "Nandi Hills Cooperative Society"),
                new Anchor("A3", "Kericho Highlands Growers"),
                new Anchor("A4", "Sotik Valley Producers"));   // 4th forces a "99. Next"
    }

    /** State with every field a node might render populated at worst case. */
    public static SessionState state() {
        SessionState state = new SessionState("254700000000");
        state.setProfile(profile());
        state.setAnchors(anchors());
        state.setSelectedAnchor(anchors().get(1));
        state.setLimit(new CreditLimit(new BigDecimal("100"), new BigDecimal("9999999")));
        state.setAmount(new BigDecimal("9999999"));
        state.setSubmissionKey("11111111-2222-3333-4444-555555555555");
        return state;
    }

    public static UssdContext context() {
        return new UssdContext("worst-case-session", state());
    }

    /** Same, but on the just-registered branch of anchor select. */
    public static UssdContext justRegisteredContext() {
        SessionState state = state();
        state.setJustRegistered(true);
        return new UssdContext("worst-case-session", state);
    }
}

package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Entry screen for a registered supplier. Input is either the literal "1" for forgot-PIN
 * or the PIN itself; a 4-digit PIN can never collide with "1".
 */
@Component
public class PinLoginNode implements MenuNode {

    private final ProfileClient profile;
    private final TradeFinanceClient tradeFinance;

    public PinLoginNode(ProfileClient profile, TradeFinanceClient tradeFinance) {
        this.profile = profile;
        this.tradeFinance = tradeFinance;
    }

    @Override
    public NodeId id() {
        return NodeId.PIN_LOGIN;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_LOGIN.formatted(
                Text.clip(ctx.state().getProfile().firstName(), Copy.NAME_MAX));
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        // Forgot PIN has no destination in the source design. This reuses the registration
        // shape: issue an OTP, close the session, and the redial lands on OTP entry
        // because Profile reports PIN_RESET_PENDING. Needs confirming.
        if ("1".equals(input)) {
            profile.beginPinReset(ctx.msisdn());
            return new Transition.Goto(NodeId.OTP_SENT);
        }

        var state = ctx.state();
        return switch (profile.verifyPin(ctx.msisdn(), input)) {
            case WRONG  -> new Transition.Goto(NodeId.PIN_LOGIN_WRONG);
            case LOCKED -> new Transition.Goto(NodeId.PIN_LOCKED);
            case OK -> {
                state.setAnchors(tradeFinance.anchorsFor(state.getProfile().supplierId()));
                yield state.getAnchors().isEmpty()
                        ? new Transition.Goto(NodeId.NO_ANCHORS)
                        : new Transition.Goto(NodeId.ANCHOR_SELECT);
            }
        };
    }
}

package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Second half of PIN creation. On a match the PIN is registered with Profile, the anchor
 * list is loaded, and the user lands on anchor select with the "You're good to go!"
 * prompt.
 */
@Component
public class PinConfirmNode implements MenuNode {

    private final ProfileClient profile;
    private final TradeFinanceClient tradeFinance;

    public PinConfirmNode(ProfileClient profile, TradeFinanceClient tradeFinance) {
        this.profile = profile;
        this.tradeFinance = tradeFinance;
    }

    @Override
    public NodeId id() {
        return NodeId.PIN_CONFIRM;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_CONFIRM;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        var state = ctx.state();
        if (input == null || !input.equals(state.getPendingPin())) {
            return new Transition.Goto(NodeId.PIN_MISMATCH);
        }

        profile.setPin(ctx.msisdn(), input);
        state.clearPendingPin();
        state.setJustRegistered(true);

        state.setAnchors(tradeFinance.anchorsFor(state.getProfile().supplierId()));
        return state.getAnchors().isEmpty()
                ? new Transition.Goto(NodeId.NO_ANCHORS)
                : new Transition.Goto(NodeId.ANCHOR_SELECT);
    }
}

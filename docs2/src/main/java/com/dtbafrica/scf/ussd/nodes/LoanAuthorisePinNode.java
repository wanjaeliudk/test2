package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import com.dtbafrica.scf.ussd.domain.LoanApplication;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * The only node that creates something irreversible, so it is the one that most needs the
 * idempotency key: a gateway timeout followed by a redial must not book two loans.
 */
@Component
public class LoanAuthorisePinNode implements MenuNode {

    private final ProfileClient profile;
    private final TradeFinanceClient tradeFinance;

    public LoanAuthorisePinNode(ProfileClient profile, TradeFinanceClient tradeFinance) {
        this.profile = profile;
        this.tradeFinance = tradeFinance;
    }

    @Override
    public NodeId id() {
        return NodeId.LOAN_AUTHORISE_PIN;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.LOAN_AUTHORISE_PIN;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        var state = ctx.state();

        return switch (profile.verifyPin(ctx.msisdn(), input)) {
            case WRONG  -> new Transition.Goto(NodeId.LOAN_AUTHORISE_WRONG_PIN);
            case LOCKED -> new Transition.Goto(NodeId.PIN_LOCKED);
            case OK -> {
                tradeFinance.submit(
                        new LoanApplication(
                                state.getProfile().supplierId(),
                                state.getSelectedAnchor().id(),
                                state.getAmount()),
                        state.getSubmissionKey());
                yield new Transition.Goto(NodeId.LOAN_SUBMITTED);
            }
        };
    }
}

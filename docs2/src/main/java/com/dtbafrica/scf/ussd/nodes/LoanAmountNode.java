package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import com.dtbafrica.scf.ussd.support.Amounts;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Amount entry. Bounds come from session state, loaded when the anchor was chosen —
 * render and handle are different HTTP requests, so the limit had to be stored.
 */
@Component
public class LoanAmountNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.LOAN_AMOUNT;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.LOAN_AMOUNT;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        var state = ctx.state();
        var limit = state.getLimit();

        var parsed = Amounts.parse(input);
        if (parsed.isEmpty() || outOfRange(parsed.get(), limit.min(), limit.max())) {
            return new Transition.Goto(NodeId.LOAN_AMOUNT_INVALID);
        }

        state.setAmount(parsed.get());
        // Minted once per accepted intent so a redial cannot create a second loan.
        if (state.getSubmissionKey() == null) {
            state.setSubmissionKey(UUID.randomUUID().toString());
        }
        return new Transition.Goto(NodeId.LOAN_AUTHORISE_PIN);
    }

    private boolean outOfRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        return amount.compareTo(min) < 0 || amount.compareTo(max) > 0;
    }
}

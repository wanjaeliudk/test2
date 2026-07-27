package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import com.dtbafrica.scf.ussd.support.Amounts;
import org.springframework.stereotype.Component;

/** Quotes the bounds, so it reads the same limit the validation used. */
@Component
public class LoanAmountInvalidNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.LOAN_AMOUNT_INVALID;
    }

    @Override
    public String render(UssdContext ctx) {
        var limit = ctx.state().getLimit();
        return Copy.LOAN_AMOUNT_INVALID.formatted(
                Amounts.format(limit.min()), Amounts.format(limit.max()));
    }
}

package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Not terminal: the design offers "00. Main Menu" here, which the engine resolves to
 * anchor select in the absence of a defined main menu.
 */
@Component
public class LoanSubmittedNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.LOAN_SUBMITTED;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.LOAN_SUBMITTED;
    }
}

package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

@Component
public class LoanAuthoriseWrongPinNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.LOAN_AUTHORISE_WRONG_PIN;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.WRONG_PIN;
    }
}

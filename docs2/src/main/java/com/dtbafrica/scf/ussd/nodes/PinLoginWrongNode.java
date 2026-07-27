package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

@Component
public class PinLoginWrongNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.PIN_LOGIN_WRONG;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.WRONG_PIN;
    }
}

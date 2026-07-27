package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

@Component
public class PinSetInvalidNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.PIN_SET_INVALID;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_SET_INVALID;
    }
}

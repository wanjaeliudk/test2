package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

@Component
public class NotRecognisedNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.NOT_RECOGNISED;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.NOT_RECOGNISED;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        return "1".equals(input)
                ? new Transition.Goto(NodeId.SUPPORT)
                : new Transition.Replace(id());
    }
}

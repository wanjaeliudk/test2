package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Reached when Profile reports a lockout. Neither source diagram contains this screen,
 * so the wording is a placeholder pending copy sign-off.
 */
@Component
public class PinLockedNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.PIN_LOCKED;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_LOCKED;
    }

    @Override
    public Transition next(String input, UssdContext ctx) {
        return switch (input) {
            case "1" -> new Transition.Goto(NodeId.SUPPORT);
            case "0" -> new Transition.Goto(NodeId.EXIT);
            default  -> new Transition.Replace(id());
        };
    }
}

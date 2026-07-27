package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Overrides {@code next} because this screen offers "0. Exit", not "0. Back" — the one
 * place where the standard navigation would be wrong.
 */
@Component
public class NoDtbAccountNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.NO_DTB_ACCOUNT;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.NO_DTB_ACCOUNT;
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

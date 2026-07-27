package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Trade finance returned an empty anchor list. Not in the source design, so the wording
 * is a placeholder — but the state is reachable and needs somewhere to land.
 */
@Component
public class NoAnchorsNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.NO_ANCHORS;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.NO_ANCHORS;
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

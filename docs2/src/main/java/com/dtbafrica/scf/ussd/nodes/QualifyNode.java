package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import com.dtbafrica.scf.ussd.support.Amounts;
import org.springframework.stereotype.Component;

/** Shows the per-anchor limit loaded when the anchor was chosen. */
@Component
public class QualifyNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.QUALIFY;
    }

    @Override
    public String render(UssdContext ctx) {
        var state = ctx.state();
        return Copy.QUALIFY.formatted(
                Amounts.format(state.getLimit().max()),
                Text.clip(state.getSelectedAnchor().name(), Copy.ANCHOR_NAME_MAX));
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        return "1".equals(input)
                ? new Transition.Goto(NodeId.LOAN_AMOUNT)
                : new Transition.Replace(id());
    }
}

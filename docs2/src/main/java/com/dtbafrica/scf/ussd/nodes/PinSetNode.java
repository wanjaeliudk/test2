package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/** First half of PIN creation. Format is checked locally; no call needed yet. */
@Component
public class PinSetNode implements MenuNode {

    static final int PIN_LENGTH = 4;

    @Override
    public NodeId id() {
        return NodeId.PIN_SET;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_SET;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        if (!isWellFormed(input)) {
            return new Transition.Goto(NodeId.PIN_SET_INVALID);
        }
        ctx.state().setPendingPin(input);
        return new Transition.Goto(NodeId.PIN_CONFIRM);
    }

    static boolean isWellFormed(String pin) {
        return pin != null
                && pin.length() == PIN_LENGTH
                && pin.chars().allMatch(Character::isDigit);
    }
}

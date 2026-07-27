package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * "0. Back" returns to the confirmation screen so the user can retry confirming. The
 * source diagram is ambiguous about whether it should instead restart at PIN entry.
 */
@Component
public class PinMismatchNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.PIN_MISMATCH;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.PIN_MISMATCH;
    }
}

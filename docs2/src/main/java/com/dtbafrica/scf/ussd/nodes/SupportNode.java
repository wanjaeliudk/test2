package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/** Contact details. "0. Back" and "00. Main Menu" both fall through to standard nav. */
@Component
public class SupportNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.SUPPORT;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.SUPPORT;
    }
}

package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Terminal sign-off for the "0. Exit" options. Exists because Infobip requires menu text
 * on every response, including the one that closes the session. Wording is a placeholder.
 */
@Component
public class ExitNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.EXIT;
    }

    @Override
    public boolean terminal() {
        return true;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.EXIT;
    }
}

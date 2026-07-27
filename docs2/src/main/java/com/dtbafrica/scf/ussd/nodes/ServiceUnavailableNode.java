package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Where the engine sends any downstream failure, rather than returning a 500 to the
 * gateway. Wording is a placeholder — no approved failure copy exists.
 */
@Component
public class ServiceUnavailableNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.SERVICE_UNAVAILABLE;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.SERVICE_UNAVAILABLE;
    }
}

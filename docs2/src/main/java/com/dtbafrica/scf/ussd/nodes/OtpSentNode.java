package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Terminal. The session must close so the SMS can arrive — registration continues in a
 * second session when the user dials back.
 */
@Component
public class OtpSentNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.OTP_SENT;
    }

    @Override
    public boolean terminal() {
        return true;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.OTP_SENT;
    }
}

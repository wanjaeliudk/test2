package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/** "0. Back" returns to OTP entry, because Goto pushed it onto the stack. */
@Component
public class OtpWrongNode implements MenuNode {

    @Override
    public NodeId id() {
        return NodeId.OTP_WRONG;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.OTP_WRONG;
    }
}

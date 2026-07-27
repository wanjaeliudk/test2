package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

@Component
public class OtpEntryNode implements MenuNode {

    private final ProfileClient profile;

    public OtpEntryNode(ProfileClient profile) {
        this.profile = profile;
    }

    @Override
    public NodeId id() {
        return NodeId.OTP_ENTRY;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.OTP_ENTRY;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        return switch (profile.verifyOtp(ctx.msisdn(), input)) {
            case OK      -> new Transition.Goto(NodeId.PIN_SET);
            case WRONG   -> new Transition.Goto(NodeId.OTP_WRONG);
            case EXPIRED -> new Transition.Goto(NodeId.OTP_EXPIRED);
        };
    }
}

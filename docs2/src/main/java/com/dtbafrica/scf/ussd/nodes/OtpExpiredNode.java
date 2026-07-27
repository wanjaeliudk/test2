package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Resending issues a fresh OTP and closes the session again, so the user redials with the
 * new code. Same two-session shape as first registration.
 */
@Component
public class OtpExpiredNode implements MenuNode {

    private final ProfileClient profile;

    public OtpExpiredNode(ProfileClient profile) {
        this.profile = profile;
    }

    @Override
    public NodeId id() {
        return NodeId.OTP_EXPIRED;
    }

    @Override
    public String render(UssdContext ctx) {
        return Copy.OTP_EXPIRED;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        if ("1".equals(input)) {
            profile.requestOtp(ctx.msisdn());
            return new Transition.Goto(NodeId.OTP_SENT);
        }
        return new Transition.Replace(id());
    }
}

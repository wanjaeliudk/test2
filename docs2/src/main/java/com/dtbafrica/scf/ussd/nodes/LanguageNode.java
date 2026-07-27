package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.menu.*;
import org.springframework.stereotype.Component;

/**
 * Entry screen for anyone not yet registered. The three-way eligibility branch happens
 * on the reply, using the profile snapshot already loaded on start — no second lookup.
 */
@Component
public class LanguageNode implements MenuNode {

    private final ProfileClient profile;

    public LanguageNode(ProfileClient profile) {
        this.profile = profile;
    }

    @Override
    public NodeId id() {
        return NodeId.LANGUAGE;
    }

    @Override
    public String render(UssdContext ctx) {
        return Features.KISWAHILI_ENABLED
                ? Copy.LANGUAGE_WITH_SWAHILI
                : Copy.LANGUAGE_EN_ONLY;
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        boolean languageChosen = "1".equals(input)
                || (Features.KISWAHILI_ENABLED && "2".equals(input));
        if (!languageChosen) {
            return new Transition.Replace(id());
        }

        return switch (ctx.state().getProfile().status()) {
            case NOT_A_SUPPLIER -> new Transition.Goto(NodeId.NOT_RECOGNISED);
            case NO_DTB_ACCOUNT -> new Transition.Goto(NodeId.NO_DTB_ACCOUNT);
            case ELIGIBLE -> {
                profile.requestOtp(ctx.msisdn());
                yield new Transition.Goto(NodeId.OTP_SENT);
            }
            // Reachable if status changes between start and this reply.
            case OTP_PENDING, PIN_RESET_PENDING -> new Transition.Goto(NodeId.OTP_ENTRY);
            case REGISTERED -> new Transition.Goto(NodeId.PIN_LOGIN);
        };
    }
}

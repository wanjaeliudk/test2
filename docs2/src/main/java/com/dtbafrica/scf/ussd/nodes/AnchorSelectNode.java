package com.dtbafrica.scf.ussd.nodes;

import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import com.dtbafrica.scf.ussd.domain.Anchor;
import com.dtbafrica.scf.ussd.menu.*;
import com.dtbafrica.scf.ussd.support.Pager;
import com.dtbafrica.scf.ussd.support.Selection;
import org.springframework.stereotype.Component;

/**
 * Anchor list, paginated. The design shows one anchor and no paging convention, so "99.
 * Next" is added here — a real supplier list will not fit one screen.
 *
 * <p>Paging uses Replace rather than Goto so the back-stack does not fill with page views.
 */
@Component
public class AnchorSelectNode implements MenuNode {

    /** Tuned against the screen budget; ScreenBudgetTest holds it honest. */
    static final int PAGE_SIZE = 3;

    private final TradeFinanceClient tradeFinance;

    public AnchorSelectNode(TradeFinanceClient tradeFinance) {
        this.tradeFinance = tradeFinance;
    }

    @Override
    public NodeId id() {
        return NodeId.ANCHOR_SELECT;
    }

    @Override
    public String render(UssdContext ctx) {
        var state = ctx.state();
        Pager.Page<Anchor> page =
                Pager.slice(state.getAnchors(), state.getAnchorPage(), PAGE_SIZE);

        StringBuilder out = new StringBuilder(prompt(ctx));
        for (int i = 0; i < page.items().size(); i++) {
            out.append("\n%d. %s".formatted(
                    i + 1, Text.clip(page.items().get(i).name(), Copy.ANCHOR_NAME_MAX)));
        }
        if (page.hasNext()) {
            out.append(Copy.NEXT);
        }
        if (!state.isJustRegistered()) {
            out.append(Copy.BACK);
        }
        return out.toString();
    }

    private String prompt(UssdContext ctx) {
        var state = ctx.state();
        if (state.isJustRegistered()) {
            return Copy.ANCHOR_PROMPT_NEW;
        }
        return Copy.ANCHOR_PROMPT_RETURNING.formatted(
                Text.clip(state.getProfile().firstName(), Copy.NAME_MAX));
    }

    @Override
    public Transition handle(String input, UssdContext ctx) {
        var state = ctx.state();
        if ("99".equals(input)) {
            state.nextAnchorPage();
            return new Transition.Replace(id());
        }

        Pager.Page<Anchor> page =
                Pager.slice(state.getAnchors(), state.getAnchorPage(), PAGE_SIZE);

        return Selection.index(input, page.items().size())
                .<Transition>map(i -> {
                    Anchor chosen = page.items().get(i);
                    state.setSelectedAnchor(chosen);
                    state.setLimit(tradeFinance.limitFor(
                            state.getProfile().supplierId(), chosen.id()));
                    return new Transition.Goto(NodeId.QUALIFY);
                })
                .orElseGet(() -> new Transition.Replace(id()));
    }
}

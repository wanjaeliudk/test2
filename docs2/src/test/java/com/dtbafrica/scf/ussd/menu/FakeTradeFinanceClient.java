package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import com.dtbafrica.scf.ussd.domain.Anchor;
import com.dtbafrica.scf.ussd.domain.CreditLimit;
import com.dtbafrica.scf.ussd.domain.LoanApplication;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FakeTradeFinanceClient implements TradeFinanceClient {

    public record Submission(LoanApplication application, String idempotencyKey) {}

    public List<Anchor> anchors = new ArrayList<>(List.of(new Anchor("A1", "Kabianga")));
    public CreditLimit limit = new CreditLimit(new BigDecimal("100"), new BigDecimal("45789"));
    public final List<Submission> submissions = new ArrayList<>();

    @Override public List<Anchor> anchorsFor(String supplierId) { return anchors; }
    @Override public CreditLimit limitFor(String supplierId, String anchorId) { return limit; }

    @Override
    public void submit(LoanApplication application, String idempotencyKey) {
        submissions.add(new Submission(application, idempotencyKey));
    }
}

package com.dtbafrica.scf.ussd.client;

import com.dtbafrica.scf.ussd.domain.Anchor;
import com.dtbafrica.scf.ussd.domain.CreditLimit;
import com.dtbafrica.scf.ussd.domain.LoanApplication;

import java.util.List;

/** Port for the Trade finance service: anchors, limits, applications. */
public interface TradeFinanceClient {

    List<Anchor> anchorsFor(String supplierId);

    CreditLimit limitFor(String supplierId, String anchorId);

    /**
     * @param idempotencyKey stable for one user intent, so a redial after a timeout
     *                       cannot create a second loan
     */
    void submit(LoanApplication application, String idempotencyKey);
}

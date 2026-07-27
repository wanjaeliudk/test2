package com.dtbafrica.scf.ussd.domain;

import java.math.BigDecimal;

public record LoanApplication(String supplierId, String anchorId, BigDecimal amount) {}

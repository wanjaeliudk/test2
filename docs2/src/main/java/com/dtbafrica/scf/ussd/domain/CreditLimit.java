package com.dtbafrica.scf.ussd.domain;

import java.math.BigDecimal;

/** Borrowing bounds for one (supplier, anchor) pair. */
public record CreditLimit(BigDecimal min, BigDecimal max) {}

package com.dtbafrica.scf.ussd.support;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Optional;

/**
 * Amount parsing and display. The mock shows "20,000" typed with a separator, but
 * feature-phone entry is digits only, so separators are stripped rather than rejected.
 */
public final class Amounts {

    private Amounts() {}

    public static Optional<BigDecimal> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String digits = input.replaceAll("[,\\s]", "");
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(digits));
    }

    /** Thousands-separated, no decimals — matches the design copy. */
    public static String format(BigDecimal amount) {
        return new DecimalFormat("#,##0").format(amount);
    }
}

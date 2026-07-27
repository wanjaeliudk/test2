package com.dtbafrica.scf.ussd.menu;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * USSD screens are GSM 03.38 7-bit encoded. Two consequences the plain string length
 * misses: characters in the extension table occupy two septets, and anything outside
 * both tables is not representable at all.
 *
 * <p>This matters in practice because design copy pasted from a document tends to carry
 * curly quotes and en-dashes, which are not GSM-7 and silently break the budget.
 */
public final class Gsm7 {

    /** Maximum septets in one USSD screen. Some networks enforce 160. */
    public static final int SCREEN_LIMIT = 182;

    private static final String BASIC =
            "@£$¥èéùìòÇ\nØø\rÅå"
            + "Δ_ΦΓΛΩΠΨΣΘΞ"
            + "ÆæßÉ"
            + " !\"#¤%&'()*+,-./0123456789:;<=>?"
            + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§"
            + "¿abcdefghijklmnopqrstuvwxyzäöñüà";

    /** Each of these costs two septets. */
    private static final String EXTENDED = "^{}\\[~]|€";

    private Gsm7() {}

    /** Septets consumed, counting extension characters as two. */
    public static int encodedLength(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (EXTENDED.indexOf(c) >= 0) {
                n += 2;
            } else if (BASIC.indexOf(c) >= 0) {
                n += 1;
            } else {
                n += 1; // unrepresentable; reported separately by unsupportedChars
            }
        }
        return n;
    }

    /** Characters that cannot be GSM-7 encoded at all. */
    public static Set<Character> unsupportedChars(String text) {
        Set<Character> bad = new LinkedHashSet<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (BASIC.indexOf(c) < 0 && EXTENDED.indexOf(c) < 0) {
                bad.add(c);
            }
        }
        return bad;
    }
}

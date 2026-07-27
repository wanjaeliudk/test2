package com.dtbafrica.scf.ussd.menu;

/** Bounded interpolation, so a long name or anchor cannot blow the screen budget. */
public final class Text {

    private Text() {}

    /** Truncate to {@code max}, marking elision with a full stop. */
    public static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + ".";
    }
}

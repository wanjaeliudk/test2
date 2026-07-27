package com.dtbafrica.scf.ussd.menu;

/** Scope switches for work that is deliberately deferred. */
public final class Features {

    /**
     * Kiswahili is deferred: the entry screen offers it in the design but no Swahili copy
     * deck exists. The option is therefore not rendered — silently continuing in English
     * after a user explicitly chose Swahili is worse than not offering it. Flip this
     * once the copy deck lands.
     */
    public static final boolean KISWAHILI_ENABLED = false;

    private Features() {}
}

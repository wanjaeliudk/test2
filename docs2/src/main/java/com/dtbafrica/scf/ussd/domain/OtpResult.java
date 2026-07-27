package com.dtbafrica.scf.ussd.domain;

/**
 * WRONG and EXPIRED must be distinguishable — the design shows a different screen for
 * each, so a boolean from the Profile contract would not be enough.
 */
public enum OtpResult { OK, WRONG, EXPIRED }

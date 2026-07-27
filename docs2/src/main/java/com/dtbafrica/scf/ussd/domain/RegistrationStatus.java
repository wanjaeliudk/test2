package com.dtbafrica.scf.ussd.domain;

/**
 * What the Profile service knows about a caller. Resolved once per session on
 * {@code start} — see PROJECT_UNDERSTANDING.md section 10.
 *
 * <p>The two "pending" states are not cosmetic. Registration deliberately spans two
 * USSD sessions: the OTP arrives by SMS after the first session closes. So on redial the
 * app must be able to tell "supplier who still needs an OTP" from "supplier whose OTP is
 * already outstanding". Without that distinction a redial re-issues an OTP every time and
 * the user can never reach the entry screen.
 */
public enum RegistrationStatus {

    /** MSISDN is not a known supplier. */
    NOT_A_SUPPLIER,

    /** Known supplier, but holds no DTB account. */
    NO_DTB_ACCOUNT,

    /** Known, eligible, no OTP outstanding — issue one. */
    ELIGIBLE,

    /** OTP already issued and unconsumed — go straight to entry. */
    OTP_PENDING,

    /** Fully registered, has a PIN. */
    REGISTERED,

    /** Registered, but a PIN reset is in flight — OTP entry, then set a new PIN. */
    PIN_RESET_PENDING
}

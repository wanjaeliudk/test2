package com.dtbafrica.scf.ussd.menu;

/**
 * Every screen in the service. An enum rather than strings or a JSON file: a node that
 * does not exist cannot be referenced, and MenuRegistry verifies at startup that each
 * constant has an implementation.
 */
public enum NodeId {

    // --- onboarding (journey 1) ---
    LANGUAGE,
    NOT_RECOGNISED,
    NO_DTB_ACCOUNT,
    SUPPORT,
    OTP_SENT,
    OTP_ENTRY,
    OTP_WRONG,
    OTP_EXPIRED,
    PIN_SET,
    PIN_SET_INVALID,
    PIN_CONFIRM,
    PIN_MISMATCH,

    // --- returning user (journey 2) ---
    PIN_LOGIN,
    PIN_LOGIN_WRONG,
    PIN_LOCKED,

    // --- loan application (shared tail) ---
    ANCHOR_SELECT,
    NO_ANCHORS,
    QUALIFY,
    LOAN_AMOUNT,
    LOAN_AMOUNT_INVALID,
    LOAN_AUTHORISE_PIN,
    LOAN_AUTHORISE_WRONG_PIN,
    LOAN_SUBMITTED,

    // --- shared ---
    SERVICE_UNAVAILABLE,
    EXIT
}

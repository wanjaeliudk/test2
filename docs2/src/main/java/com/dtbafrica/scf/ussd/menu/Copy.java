package com.dtbafrica.scf.ussd.menu;

/**
 * All user-facing text, in one place, so the deferred Kiswahili retrofit is a lookup
 * swap rather than an edit of every node.
 *
 * <p>Straight quotes and hyphens only — curly punctuation is not GSM-7 encodable and
 * Gsm7Test enforces that.
 *
 * <h2>Deviations from the approved design</h2>
 * <ul>
 *   <li>{@link #NO_DTB_ACCOUNT} was reworded. As drawn it is 187 septets, over the 182
 *       limit, and would have been truncated on the handset. Meaning is preserved.
 *   <li>{@code "The OTP you eneterd has expired"} in the mock is a typo; corrected here.
 * </ul>
 *
 * <h2>Invented copy — needs sign-off</h2>
 * {@link #PIN_LOCKED}, {@link #NO_ANCHORS}, {@link #SERVICE_UNAVAILABLE} and
 * {@link #EXIT} have no approved wording. Each is a state the flow can genuinely reach,
 * so a placeholder is better than a dead end, but the words are mine.
 */
public final class Copy {

    private Copy() {}

    // Bounds on interpolated values, so a long name cannot blow the budget.
    public static final int NAME_MAX = 12;
    public static final int ANCHOR_NAME_MAX = 18;

    // --- shared option lines ---
    public static final String BACK = "\n0. Back";
    public static final String MAIN_MENU = "\n00. Main Menu";
    public static final String NEXT = "\n99. Next";
    public static final String CONTACT_SUPPORT = "\n1. Contact customer support";

    // --- onboarding ---
    public static final String LANGUAGE_EN_ONLY =
            "Hello and welcome to DTB Agriloans."
            + "\n1. Proceed in English";

    public static final String LANGUAGE_WITH_SWAHILI =
            "Hello and welcome to DTB Agriloans."
            + "\n1. Proceed in English"
            + "\n2. Endelea kwa Kiswahili";

    public static final String NOT_RECOGNISED =
            "You do not qualify for DTB Agriloans."
            + CONTACT_SUPPORT
            + BACK;

    /** Reworded from the design: the original renders 187 septets. */
    public static final String NO_DTB_ACCOUNT =
            "Your number is registered but you need a DTB account for DTB Agriloans."
            + "\nPlease contact support or visit the nearest branch."
            + CONTACT_SUPPORT
            + "\n0. Exit";

    public static final String SUPPORT =
            "We are here to support you around the clock, 24/7."
            + "\nEmail us on : contactcentre@dtbafrica.com"
            + "\nCall us on :"
            + "\n+254 202849888"
            + "\n+254 719031888"
            + "\n+254 732121888"
            + BACK
            + MAIN_MENU;

    public static final String OTP_SENT =
            "An OTP has been sent to you. Exit now, check your messages, "
            + "and dial back as soon as possible.";

    public static final String OTP_ENTRY =
            "Please enter the OTP that was sent to you." + BACK;

    public static final String OTP_WRONG =
            "Wrong OTP. Please try again." + BACK;

    public static final String OTP_EXPIRED =
            "The OTP you entered has expired."
            + "\n1. Resend OTP.";

    public static final String PIN_SET =
            "Set a new PIN. Should be 4 digits." + BACK;

    public static final String PIN_SET_INVALID =
            "Invalid PIN, please try again. PIN should be 4 digits." + BACK;

    public static final String PIN_CONFIRM =
            "Confirm your new PIN." + BACK;

    public static final String PIN_MISMATCH =
            "PINs don't match. Please try again." + BACK;

    // --- returning user ---
    /** %s = first name */
    public static final String PIN_LOGIN =
            "Hi %s. Please enter your PIN to proceed."
            + "\n1. Forgot PIN";

    public static final String WRONG_PIN =
            "Wrong PIN, please try again." + BACK;

    /** Invented. No approved copy for the lockout state. */
    public static final String PIN_LOCKED =
            "Your PIN is locked after too many attempts."
            + CONTACT_SUPPORT
            + "\n0. Exit";

    // --- loan application ---
    /** %s = first name */
    public static final String ANCHOR_PROMPT_RETURNING =
            "Hi %s, welcome to DTB Supply Chain Finance. Please select an anchor.";

    public static final String ANCHOR_PROMPT_NEW =
            "You're good to go! Please select an anchor.";

    /** Invented. Reachable whenever Trade finance returns an empty anchor list. */
    public static final String NO_ANCHORS =
            "You have no active anchors right now."
            + CONTACT_SUPPORT
            + "\n0. Exit";

    /** %1$s = limit, %2$s = anchor name */
    public static final String QUALIFY =
            "You qualify for a loan of upto KES %1$s from %2$s."
            + "\n1. Apply for loan"
            + BACK;

    public static final String LOAN_AMOUNT =
            "How much do you want borrow?" + BACK;

    /** %1$s = min, %2$s = max */
    public static final String LOAN_AMOUNT_INVALID =
            "Please try again. The amount must be between KES %1$s and KES %2$s."
            + BACK;

    public static final String LOAN_AUTHORISE_PIN =
            "Please enter your PIN to authorise loan application." + BACK;

    public static final String LOAN_SUBMITTED =
            "Your request has been received and is being processed." + MAIN_MENU;

    // --- shared failure states ---
    /** Invented. Every outbound call can fail and USSD has no retry affordance. */
    public static final String SERVICE_UNAVAILABLE =
            "Sorry, we cannot process this right now. Please try again shortly."
            + BACK;

    /** Invented. "0. Exit" needs a final screen because Infobip requires menu text. */
    public static final String EXIT =
            "Thank you for using DTB Agriloans.";
}

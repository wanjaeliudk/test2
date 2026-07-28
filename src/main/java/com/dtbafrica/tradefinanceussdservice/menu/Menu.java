package com.dtbafrica.tradefinanceussdservice.menu;

public class Menu {

  public static final String MAIN_MENU = "\n00. Main Menu";
  public static final String NEXT = "\n98. Next";
  public static final String BACK = "\n0. Back";
  public static final String CONTACT_SUPPORT = "\n1. Contact Support";

  // Onboarding Menu
  public static final String ONBOARDING_LANGUAGE =
      "Hello and welcome to DTB Agriloans."
          + "\n Please select your preferred language:"
          + "\n1. English"
          + "\n2. Kiswahili";

  public static String SUPPLIER_PROFILE_NOT_RECOGNIZED =
      "You do not qualify for DTB Agriloans." + CONTACT_SUPPORT + BACK;

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

  public static final String OTP_ENTRY = "Please enter the OTP sent to you." + BACK;

  public static final String OTP_WRONG = "Wrong OTP. Please try again." + BACK;

  public static final String OTP_EXPIRED = "The OTP you entered has expired." + "\n1. Resend OTP.";

  public static final String PIN_SET = "Set a new PIN. Should be 4 digits." + BACK;

  public static final String PIN_SET_INVALID =
      "Invalid PIN, please try again. PIN should be 4 digits." + BACK;

  public static final String PIN_CONFIRM = "Confirm your new PIN." + BACK;

  public static final String PIN_MISMATCH = "PINs don't match. Please try again." + BACK;

  // --- returning user ---
  // %s = first name
  public static final String PIN_LOGIN =
      "Hi %s. Please enter your PIN to proceed." + "\n1. Forgot PIN";

  public static final String WRONG_PIN = "Wrong PIN, please try again." + BACK;

  // Invented. No approved copy for the lockout state.
  public static final String PIN_LOCKED =
      "Your PIN is locked after too many attempts." + CONTACT_SUPPORT + "\n0. Exit";

  // --- loan application ---
  // %s = first name
  public static final String ANCHOR_PROMPT_RETURNING =
      "Hi %s, welcome to DTB Supply Chain Finance. Please select an anchor.";

  public static final String ANCHOR_PROMPT_NEW = "You're good to go! Please select an anchor.";

  // Reachable whenever Trade finance returns an empty anchor list.
  public static final String NO_ANCHORS =
      "You have no active anchors right now." + CONTACT_SUPPORT + "\n0. Exit";

  // %1$s = loan limit, %2$s = anchor name
  public static final String QUALIFY =
      "You qualify for a loan of upto KES %1$s from %2$s." + "\n1. Apply for loan" + BACK;

  public static final String LOAN_AMOUNT = "How much do you want borrow?" + BACK;

  // %s = min (100), %s = max (Loan Limit)
  public static final String LOAN_AMOUNT_INVALID =
      "Please try again. The amount must be between KES 100 and KES %s." + BACK;

  public static final String LOAN_AUTHORISE_PIN =
      "Please enter your PIN to authorise loan application." + BACK;

  public static final String LOAN_SUBMITTED =
      "Your request has been received and is being processed." + MAIN_MENU;

  // transient error
  public static final String SERVICE_UNAVAILABLE =
      "Sorry, we cannot process this right now. Please try again shortly." + BACK;

  public static final String EXIT = "Thank you for using DTB Agriloans.";
}

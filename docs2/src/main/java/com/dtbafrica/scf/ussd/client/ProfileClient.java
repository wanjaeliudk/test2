package com.dtbafrica.scf.ussd.client;

import com.dtbafrica.scf.ussd.domain.OtpResult;
import com.dtbafrica.scf.ussd.domain.PinResult;
import com.dtbafrica.scf.ussd.domain.ProfileSnapshot;

/**
 * Port for the Profile service, which owns the whole identity and credential journey.
 * Deliberately an interface: the real contract has not landed, so the menus are written
 * against the capability required and the HTTP client is fitted afterwards.
 */
public interface ProfileClient {

    /** The single lookup on session start. */
    ProfileSnapshot resolveByMsisdn(String msisdn);

    /** Ask Profile to issue and send an OTP by SMS. */
    void requestOtp(String msisdn);

    OtpResult verifyOtp(String msisdn, String code);

    /**
     * Set a first PIN, or replace one after a reset.
     *
     * <p>If Profile can own the two-step set-and-confirm instead, prefer that — it would
     * remove the need to hold the first entry in our session store.
     */
    void setPin(String msisdn, String pin);

    PinResult verifyPin(String msisdn, String pin);

    /** Begin a PIN reset: issues an OTP and moves the caller to PIN_RESET_PENDING. */
    void beginPinReset(String msisdn);
}

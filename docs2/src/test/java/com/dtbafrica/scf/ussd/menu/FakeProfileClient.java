package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.domain.*;

public class FakeProfileClient implements ProfileClient {

    public ProfileSnapshot snapshot =
            new ProfileSnapshot("SUP-1", "Kamau", RegistrationStatus.REGISTERED, "en");
    public OtpResult otpResult = OtpResult.OK;
    public PinResult pinResult = PinResult.OK;
    public RuntimeException failResolveWith;

    public int otpRequests;
    public int pinSets;
    public int pinResets;

    @Override
    public ProfileSnapshot resolveByMsisdn(String msisdn) {
        if (failResolveWith != null) {
            throw failResolveWith;
        }
        return snapshot;
    }

    @Override public void requestOtp(String msisdn) { otpRequests++; }
    @Override public OtpResult verifyOtp(String msisdn, String code) { return otpResult; }
    @Override public void setPin(String msisdn, String pin) { pinSets++; }
    @Override public PinResult verifyPin(String msisdn, String pin) { return pinResult; }
    @Override public void beginPinReset(String msisdn) { pinResets++; }
}

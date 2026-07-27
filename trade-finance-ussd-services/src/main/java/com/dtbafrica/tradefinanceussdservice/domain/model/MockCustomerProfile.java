package com.dtbafrica.tradefinanceussdservice.domain.model;

public class MockCustomerProfile {

  private String msisdn;
  private String name;
  private String pin;

  public MockCustomerProfile(String msisdn, String name, String pin) {
    this.msisdn = msisdn;
    this.name = name;
    this.pin = pin;
  }

  public String msisdn() {
    return msisdn;
  }

  public String name() {
    return name;
  }

  public String pin() {
    return pin;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public String getName() {
    return name;
  }

  public String getPin() {
    return pin;
  }
}

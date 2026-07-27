package com.dtbafrica.tradefinanceussdservice.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UssdResponseRequest {

  @NotBlank(message = "msisdn is required")
  private String msisdn;

  private String imsi;
  private String shortCode;

  @NotBlank(message = "text is required")
  private String text;

  private String ussdNodeId;
  private String networkName;
  private String countryName;

  public UssdResponseRequest() {}

  public UssdResponseRequest(
      String msisdn,
      String imsi,
      String shortCode,
      String text,
      String ussdNodeId,
      String networkName,
      String countryName) {
    this.msisdn = msisdn;
    this.imsi = imsi;
    this.shortCode = shortCode;
    this.text = text;
    this.ussdNodeId = ussdNodeId;
    this.networkName = networkName;
    this.countryName = countryName;
  }

  public String msisdn() {
    return msisdn;
  }

  public String imsi() {
    return imsi;
  }

  public String shortCode() {
    return shortCode;
  }

  public String text() {
    return text;
  }

  public String ussdNodeId() {
    return ussdNodeId;
  }

  public String networkName() {
    return networkName;
  }

  public String countryName() {
    return countryName;
  }

  public void setMsisdn(String msisdn) {
    this.msisdn = msisdn;
  }

  public void setImsi(String imsi) {
    this.imsi = imsi;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setUssdNodeId(String ussdNodeId) {
    this.ussdNodeId = ussdNodeId;
  }

  public void setNetworkName(String networkName) {
    this.networkName = networkName;
  }

  public void setCountryName(String countryName) {
    this.countryName = countryName;
  }
}

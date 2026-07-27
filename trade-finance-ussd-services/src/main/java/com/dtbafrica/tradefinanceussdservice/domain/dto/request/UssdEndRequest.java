package com.dtbafrica.tradefinanceussdservice.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UssdEndRequest {

  private String reason;
  private Integer exitCode;

  public UssdEndRequest() {}

  public UssdEndRequest(String reason, Integer exitCode) {
    this.reason = reason;
    this.exitCode = exitCode;
  }

  public String reason() {
    return reason;
  }

  public Integer exitCode() {
    return exitCode;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public void setExitCode(Integer exitCode) {
    this.exitCode = exitCode;
  }
}

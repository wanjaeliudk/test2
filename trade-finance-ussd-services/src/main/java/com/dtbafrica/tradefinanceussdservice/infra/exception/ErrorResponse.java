package com.dtbafrica.tradefinanceussdservice.infra.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

  private String message;
  private String path;

  public ErrorResponse() {}

  public ErrorResponse(String message, String path) {
    this.message = message;
    this.path = path;
  }
}

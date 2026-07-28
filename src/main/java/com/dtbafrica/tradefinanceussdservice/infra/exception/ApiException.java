package com.dtbafrica.tradefinanceussdservice.infra.exception;

public class ApiException extends RuntimeException {

  public ApiException(String message) {
    super(message);
  }
}

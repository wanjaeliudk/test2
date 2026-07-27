package com.dtbafrica.tradefinanceussdservice.domain.dto.response;

public class UssdGatewayResponse {

  private boolean shouldClose;
  private String ussdMenu;
  private int responseExitCode;
  private String responseMessage;

  public UssdGatewayResponse() {}

  public UssdGatewayResponse(
      boolean shouldClose, String ussdMenu, int responseExitCode, String responseMessage) {
    this.shouldClose = shouldClose;
    this.ussdMenu = ussdMenu;
    this.responseExitCode = responseExitCode;
    this.responseMessage = responseMessage;
  }

  public boolean shouldClose() {
    return shouldClose;
  }

  public boolean isShouldClose() {
    return shouldClose;
  }

  public String ussdMenu() {
    return ussdMenu;
  }

  public String getUssdMenu() {
    return ussdMenu;
  }

  public int responseExitCode() {
    return responseExitCode;
  }

  public int getResponseExitCode() {
    return responseExitCode;
  }

  public String responseMessage() {
    return responseMessage;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setShouldClose(boolean shouldClose) {
    this.shouldClose = shouldClose;
  }

  public void setUssdMenu(String ussdMenu) {
    this.ussdMenu = ussdMenu;
  }

  public void setResponseExitCode(int responseExitCode) {
    this.responseExitCode = responseExitCode;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }
}

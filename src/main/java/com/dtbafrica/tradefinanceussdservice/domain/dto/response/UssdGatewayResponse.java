package com.dtbafrica.tradefinanceussdservice.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UssdGatewayResponse {

  private boolean shouldClose;
  private String ussdMenu;
  private int responseExitCode;
  private String responseMessage;
}

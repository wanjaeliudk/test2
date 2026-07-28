package com.dtbafrica.tradefinanceussdservice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UssdStartRequest {

  @NotBlank(message = "msisdn is required")
  private String msisdn; // user phone number

  private String imsi;
  private String shortCode; // e.g., *126#
  private String text; // e.g., 1 for menu
  private String ussdNodeId;
  private String networkName; // e.g., Safaricom
  private String countryName; // e.g., Kenya
}

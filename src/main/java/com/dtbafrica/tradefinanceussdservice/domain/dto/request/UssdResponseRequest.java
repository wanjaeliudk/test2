package com.dtbafrica.tradefinanceussdservice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UssdResponseRequest {
  @NotBlank(message = "msisdn is required")
  private String msisdn; // user phone number

  private String imsi;
  private String shortCode; // e.g., *126#

  @NotBlank(message = "text is required")
  private String text;

  private String ussdNodeId;
  private String networkName; // Safaricom
  private String countryName; // Kenya
}

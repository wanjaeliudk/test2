package com.dtbafrica.tradefinanceussdservice.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UssdEndRequest {

  private String reason; // e.g., Request processed without error
  private Integer exitCode; // e.g., 200
  private String networkName; // e.g., Safaricom
  private String countryName; // e.g., Kenya
}

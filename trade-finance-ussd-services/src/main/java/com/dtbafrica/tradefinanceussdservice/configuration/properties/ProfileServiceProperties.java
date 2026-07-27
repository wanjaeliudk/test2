package com.dtbafrica.tradefinanceussdservice.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "profile-service")
public class ProfileServiceProperties {

  private String baseUrl;
  private Endpoints endpoints;

  @Setter
  @Getter
  public static class Endpoints {

    private String verifyOtp;
    private String sendOtp;
    private String pinStrength;
    private String changePin;
    private String resetPinById;
  }
}

package com.dtbafrica.tradefinanceussdservice.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "profile-service")
public class ProfileServiceProperties {

  private String baseUrl;
  private Endpoints endpoints;

  @Setter
  @Getter
  public static class Endpoints {
    private String checkCustomerProfile;
    private String verifyOtp;
    private String sendOtp;
    private String pinStrength;
    private String changePin;
    private String resetPinById;
  }
}

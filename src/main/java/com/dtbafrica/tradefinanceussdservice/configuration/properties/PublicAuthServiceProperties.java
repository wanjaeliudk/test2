package com.dtbafrica.tradefinanceussdservice.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-auth-service")
public class PublicAuthServiceProperties {

  private String baseUrl;
  private Endpoints endpoints;
  //    private Credentials credentials;
  private String clientId;
  private String clientSecret;
  private String grantType;

  @Getter
  @Setter
  public static class Endpoints {
    private String tokenUrl;
  }

  //    @Getter
  //    @Setter
  //    public static class Credentials {
  //        private String clientId;
  //        private String clientSecret;
  //        private String grantType;
  //    }
}

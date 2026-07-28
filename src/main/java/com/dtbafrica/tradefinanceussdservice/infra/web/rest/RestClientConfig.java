package com.dtbafrica.tradefinanceussdservice.infra.web.rest;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean(name = "restClient")
  RestClient restClient()
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    return RestClient.builder()
        .requestFactory(getHttpRequestFactory())
        .requestInterceptor(httpLogInterceptor)
        .build();
  }
}

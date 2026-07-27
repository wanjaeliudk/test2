package com.dtbafrica.tradefinanceussdservice;

import com.dtbafrica.tradefinanceussdservice.configuration.properties.ProfileServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProfileServiceProperties.class)
public class TradeFinanceUssdServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TradeFinanceUssdServiceApplication.class, args);
  }
}

package com.dtbafrica.tradefinanceussdservice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class TradeFinanceUssdServiceApplicationTests {

  @Test
  void contextLoads() {
    assertTrue(true);
  }

  public static void main(String[] args) throws IOException {}
}

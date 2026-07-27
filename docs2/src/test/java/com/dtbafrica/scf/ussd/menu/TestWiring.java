package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.client.ProfileClient;
import com.dtbafrica.scf.ussd.client.TradeFinanceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Scans the real components, so refreshing this context is itself the assertion that
 * every NodeId has an implementation — MenuRegistry throws otherwise.
 */
@Configuration
@ComponentScan(basePackages = "com.dtbafrica.scf.ussd")
public class TestWiring {

    @Bean
    ProfileClient profileClient() {
        return new FakeProfileClient();
    }

    @Bean
    TradeFinanceClient tradeFinanceClient() {
        return new FakeTradeFinanceClient();
    }
}

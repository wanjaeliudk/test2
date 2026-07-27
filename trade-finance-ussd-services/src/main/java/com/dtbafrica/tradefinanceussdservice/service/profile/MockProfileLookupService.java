package com.dtbafrica.tradefinanceussdservice.service.profile;

import com.dtbafrica.tradefinanceussdservice.domain.model.MockCustomerProfile;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MockProfileLookupService implements ProfileLookupService {

  private final Map<String, MockCustomerProfile> profiles =
      Map.of(
          "254700000001", new MockCustomerProfile("254700000001", "Amina", "1234"),
          "254700000002", new MockCustomerProfile("254700000002", "David", "4321"));

  @Override
  public Optional<MockCustomerProfile> findByMsisdn(String msisdn) {
    return Optional.ofNullable(profiles.get(msisdn));
  }
}

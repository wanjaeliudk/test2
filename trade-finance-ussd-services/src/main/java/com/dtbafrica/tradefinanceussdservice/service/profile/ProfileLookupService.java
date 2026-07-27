package com.dtbafrica.tradefinanceussdservice.service.profile;

import com.dtbafrica.tradefinanceussdservice.domain.model.MockCustomerProfile;
import java.util.Optional;

public interface ProfileLookupService {

  Optional<MockCustomerProfile> findByMsisdn(String msisdn);
}

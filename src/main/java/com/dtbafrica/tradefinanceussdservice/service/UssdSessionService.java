package com.dtbafrica.tradefinanceussdservice.service;

import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdEndRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdResponseRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdStartRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.response.UssdGatewayResponse;
import jakarta.validation.Valid;

public interface UssdSessionService {
  UssdGatewayResponse startSession(String sessionId, @Valid UssdStartRequest request);

  UssdGatewayResponse handleResponse(String sessionId, @Valid UssdResponseRequest request);

  UssdGatewayResponse endSession(String sessionId, @Valid UssdEndRequest request);
}

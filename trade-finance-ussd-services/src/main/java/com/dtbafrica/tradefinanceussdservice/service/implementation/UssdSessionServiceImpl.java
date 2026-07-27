package com.dtbafrica.tradefinanceussdservice.service.implementation;

import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdEndRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdResponseRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdStartRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.response.UssdGatewayResponse;
import com.dtbafrica.tradefinanceussdservice.service.UssdSessionService;
import com.dtbafrica.tradefinanceussdservice.service.menu.UssdJourneyEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UssdSessionServiceImpl implements UssdSessionService {

  private final UssdJourneyEngine journeyEngine;

  @Override
  public UssdGatewayResponse startSession(String sessionId, UssdStartRequest request) {
    return journeyEngine.start(sessionId, request);
  }

  @Override
  public UssdGatewayResponse handleResponse(String sessionId, UssdResponseRequest request) {
    return journeyEngine.respond(sessionId, request);
  }

  @Override
  public UssdGatewayResponse endSession(String sessionId, UssdEndRequest request) {
    return journeyEngine.end(sessionId, request);
  }
}

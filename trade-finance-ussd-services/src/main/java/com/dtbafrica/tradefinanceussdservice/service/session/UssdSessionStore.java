package com.dtbafrica.tradefinanceussdservice.service.session;

import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionState;
import java.util.Optional;

public interface UssdSessionStore {

  UssdSessionState save(UssdSessionState sessionState);

  Optional<UssdSessionState> findBySessionId(String sessionId);

  void delete(String sessionId);
}

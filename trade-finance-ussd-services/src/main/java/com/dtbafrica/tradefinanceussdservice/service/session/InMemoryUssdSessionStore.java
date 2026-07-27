package com.dtbafrica.tradefinanceussdservice.service.session;

import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
public class InMemoryUssdSessionStore implements UssdSessionStore {

  private final Map<String, UssdSessionState> sessions = new ConcurrentHashMap<>();

  @Override
  public UssdSessionState save(UssdSessionState sessionState) {
    sessions.put(sessionState.getSessionId(), sessionState);
    return sessionState;
  }

  @Override
  public Optional<UssdSessionState> findBySessionId(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  @Override
  public void delete(String sessionId) {
    sessions.remove(sessionId);
  }
}

package com.dtbafrica.tradefinanceussdservice.infra.persistence.adapter;

import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionState;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionStatus;
import com.dtbafrica.tradefinanceussdservice.infra.exception.ApiException;
import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdSessionEntity;
import com.dtbafrica.tradefinanceussdservice.infra.persistence.repository.UssdSessionRepository;
import com.dtbafrica.tradefinanceussdservice.service.session.UssdSessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class JpaUssdSessionStore implements UssdSessionStore {

  private final UssdSessionRepository repository;
  private final ObjectMapper objectMapper;

  @Override
  public UssdSessionState save(UssdSessionState sessionState) {
    UssdSessionEntity entity =
        repository.findBySessionId(sessionState.getSessionId()).orElseGet(UssdSessionEntity::new);
    entity.setSessionId(sessionState.getSessionId());
    entity.setMsisdn(sessionState.getMsisdn());
    entity.setFlowCode(sessionState.getFlowCode());
    entity.setCurrentNodeKey(sessionState.getCurrentNodeKey());
    entity.setStatus(sessionState.getStatus().name());
    entity.setHistoryJson(writeJson(sessionState.getHistory()));
    entity.setVariablesJson(writeJson(sessionState.getVariables()));
    repository.save(entity);
    return sessionState;
  }

  @Override
  public Optional<UssdSessionState> findBySessionId(String sessionId) {
    return repository.findBySessionId(sessionId).map(this::toDomain);
  }

  @Override
  public void delete(String sessionId) {
    repository.findBySessionId(sessionId).ifPresent(repository::delete);
  }

  private UssdSessionState toDomain(UssdSessionEntity entity) {
    UssdSessionState sessionState = new UssdSessionState();
    sessionState.setSessionId(entity.getSessionId());
    sessionState.setMsisdn(entity.getMsisdn());
    sessionState.setFlowCode(entity.getFlowCode());
    sessionState.setCurrentNodeKey(entity.getCurrentNodeKey());
    sessionState.setStatus(UssdSessionStatus.valueOf(entity.getStatus()));
    sessionState.getHistory().addAll(readDeque(entity.getHistoryJson()));
    sessionState.getVariables().putAll(readMap(entity.getVariablesJson()));
    return sessionState;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new ApiException("Failed to persist USSD session data");
    }
  }

  private ArrayDeque<String> readDeque(String value) {
    try {
      String[] items = objectMapper.readValue(value == null ? "[]" : value, String[].class);
      ArrayDeque<String> deque = new ArrayDeque<>();
      for (String item : items) {
        deque.add(item);
      }
      return deque;
    } catch (JsonProcessingException exception) {
      throw new ApiException("Failed to read USSD session history");
    }
  }

  private Map<String, String> readMap(String value) {
    try {
      if (value == null || value.isBlank()) {
        return new LinkedHashMap<>();
      }
      return objectMapper.readValue(
          value,
          objectMapper
              .getTypeFactory()
              .constructMapType(LinkedHashMap.class, String.class, String.class));
    } catch (JsonProcessingException exception) {
      throw new ApiException("Failed to read USSD session variables");
    }
  }
}

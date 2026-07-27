package com.dtbafrica.tradefinanceussdservice.domain.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class UssdSessionState {

  private String sessionId;
  private String msisdn;
  private String flowCode;
  private String currentNodeKey;
  private UssdSessionStatus status = UssdSessionStatus.ACTIVE;
  private final Deque<String> history = new ArrayDeque<>();
  private final Map<String, String> variables = new LinkedHashMap<>();

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public void setMsisdn(String msisdn) {
    this.msisdn = msisdn;
  }

  public String getFlowCode() {
    return flowCode;
  }

  public void setFlowCode(String flowCode) {
    this.flowCode = flowCode;
  }

  public String getCurrentNodeKey() {
    return currentNodeKey;
  }

  public void setCurrentNodeKey(String currentNodeKey) {
    this.currentNodeKey = currentNodeKey;
  }

  public UssdSessionStatus getStatus() {
    return status;
  }

  public void setStatus(UssdSessionStatus status) {
    this.status = status;
  }

  public Deque<String> getHistory() {
    return history;
  }

  public Map<String, String> getVariables() {
    return variables;
  }
}

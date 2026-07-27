package com.dtbafrica.tradefinanceussdservice.domain.model;

import java.util.Map;

public class UssdMenuOption {

  private final String inputValue;
  private final String label;
  private final String nextNodeKey;
  private final Map<String, String> contextUpdates;

  public UssdMenuOption(
      String inputValue, String label, String nextNodeKey, Map<String, String> contextUpdates) {
    this.inputValue = inputValue;
    this.label = label;
    this.nextNodeKey = nextNodeKey;
    this.contextUpdates = contextUpdates == null ? Map.of() : Map.copyOf(contextUpdates);
  }

  public String inputValue() {
    return inputValue;
  }

  public String label() {
    return label;
  }

  public String nextNodeKey() {
    return nextNodeKey;
  }

  public Map<String, String> contextUpdates() {
    return contextUpdates;
  }
}

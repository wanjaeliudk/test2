package com.dtbafrica.tradefinanceussdservice.domain.model;

import java.util.List;

public class UssdMenuNode {

  private final String key;
  private final String promptTemplate;
  private final UssdMenuInputType inputType;
  private final UssdPinMode pinMode;
  private final String variableKey;
  private final String nextNodeKey;
  private final boolean terminal;
  private final boolean closeSession;
  private final List<UssdMenuOption> options;
  private final String invalidInputMessage;

  public UssdMenuNode(
      String key,
      String promptTemplate,
      UssdMenuInputType inputType,
      UssdPinMode pinMode,
      String variableKey,
      String nextNodeKey,
      boolean terminal,
      boolean closeSession,
      List<UssdMenuOption> options,
      String invalidInputMessage) {
    this.key = key;
    this.promptTemplate = promptTemplate;
    this.inputType = inputType;
    this.pinMode = pinMode;
    this.variableKey = variableKey;
    this.nextNodeKey = nextNodeKey;
    this.terminal = terminal;
    this.closeSession = closeSession;
    this.options = options == null ? List.of() : List.copyOf(options);
    this.invalidInputMessage = invalidInputMessage == null ? "" : invalidInputMessage;
  }

  public String key() {
    return key;
  }

  public String promptTemplate() {
    return promptTemplate;
  }

  public UssdMenuInputType inputType() {
    return inputType;
  }

  public UssdPinMode pinMode() {
    return pinMode;
  }

  public String variableKey() {
    return variableKey;
  }

  public String nextNodeKey() {
    return nextNodeKey;
  }

  public boolean terminal() {
    return terminal;
  }

  public boolean closeSession() {
    return closeSession;
  }

  public List<UssdMenuOption> options() {
    return options;
  }

  public String invalidInputMessage() {
    return invalidInputMessage;
  }
}

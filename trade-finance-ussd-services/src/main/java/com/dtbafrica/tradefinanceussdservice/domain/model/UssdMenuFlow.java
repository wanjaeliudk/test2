package com.dtbafrica.tradefinanceussdservice.domain.model;

import java.util.Map;
import java.util.Optional;

public class UssdMenuFlow {

  private final String code;
  private final String startNodeKey;
  private final Map<String, UssdMenuNode> nodes;

  public UssdMenuFlow(String code, String startNodeKey, Map<String, UssdMenuNode> nodes) {
    this.code = code;
    this.startNodeKey = startNodeKey;
    this.nodes = Map.copyOf(nodes);
  }

  public String code() {
    return code;
  }

  public String startNodeKey() {
    return startNodeKey;
  }

  public Map<String, UssdMenuNode> nodes() {
    return nodes;
  }

  public Optional<UssdMenuNode> node(String nodeKey) {
    return Optional.ofNullable(nodes.get(nodeKey));
  }
}

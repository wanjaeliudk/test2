package com.dtbafrica.tradefinanceussdservice.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "ussd_session")
public class UssdSessionEntity extends BaseEntity {

  @Column(name = "session_id", nullable = false, unique = true, length = 100)
  private String sessionId;

  @Column(name = "msisdn", nullable = false, length = 20)
  private String msisdn;

  @Column(name = "flow_code", nullable = false, length = 100)
  private String flowCode;

  @Column(name = "current_node_key", nullable = false, length = 100)
  private String currentNodeKey;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "history_json", columnDefinition = "text")
  private String historyJson;

  @Column(name = "variables_json", columnDefinition = "text")
  private String variablesJson;
}

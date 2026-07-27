package com.dtbafrica.tradefinanceussdservice.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
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
@Table(name = "ussd_menu_node")
public class UssdMenuNodeEntity extends BaseEntity {

  @Column(name = "flow_id", nullable = false)
  private UUID flowId;

  @Column(name = "node_key", nullable = false, length = 100)
  private String nodeKey;

  @Column(name = "title", length = 150)
  private String title;

  @Column(name = "prompt_template", nullable = false, columnDefinition = "text")
  private String promptTemplate;

  @Column(name = "input_type", nullable = false, length = 30)
  private String inputType;

  @Column(name = "pin_mode", length = 30)
  private String pinMode;

  @Column(name = "variable_key", length = 100)
  private String variableKey;

  @Column(name = "next_node_key", length = 100)
  private String nextNodeKey;

  @Column(name = "terminal", nullable = false)
  private Boolean terminal;

  @Column(name = "close_session", nullable = false)
  private Boolean closeSession;

  @Column(name = "invalid_input_message", columnDefinition = "text")
  private String invalidInputMessage;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;
}

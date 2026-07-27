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
@Table(name = "ussd_menu_transition")
public class UssdMenuTransitionEntity extends BaseEntity {

  @Column(name = "node_id", nullable = false)
  private UUID nodeId;

  @Column(name = "transition_type", nullable = false, length = 30)
  private String transitionType;

  @Column(name = "input_value", length = 50)
  private String inputValue;

  @Column(name = "condition_expression", columnDefinition = "text")
  private String conditionExpression;

  @Column(name = "next_node_key", nullable = false, length = 100)
  private String nextNodeKey;

  @Column(name = "is_default_transition", nullable = false)
  private Boolean defaultTransition;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;
}

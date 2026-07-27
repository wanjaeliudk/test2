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
@Table(name = "ussd_menu_option")
public class UssdMenuOptionEntity extends BaseEntity {

  @Column(name = "node_id", nullable = false)
  private UUID nodeId;

  @Column(name = "input_value", nullable = false, length = 50)
  private String inputValue;

  @Column(name = "label_template", nullable = false, columnDefinition = "text")
  private String labelTemplate;

  @Column(name = "next_node_key", nullable = false, length = 100)
  private String nextNodeKey;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "context_updates_json", columnDefinition = "text")
  private String contextUpdatesJson;
}

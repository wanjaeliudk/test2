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
@Table(name = "ussd_menu_flow")
public class UssdMenuFlowEntity extends BaseEntity {

  @Column(name = "code", nullable = false, length = 100)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "start_node_key", nullable = false, length = 100)
  private String startNodeKey;
}

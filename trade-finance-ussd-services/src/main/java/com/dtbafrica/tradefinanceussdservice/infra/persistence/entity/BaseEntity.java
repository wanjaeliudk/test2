package com.dtbafrica.tradefinanceussdservice.infra.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
@EntityListeners({AuditingEntityListener.class})
public abstract class BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @CreatedDate
  @Column(
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime dateCreated;

  @LastModifiedDate
  @Column(nullable = false, columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime dateModified;

  @CreatedBy private String createdBy;

  @LastModifiedBy private String updatedBy;
}

package com.dtbafrica.tradefinanceussdservice.infra.persistence.repository;

import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdMenuFlowEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdMenuFlowRepository extends JpaRepository<UssdMenuFlowEntity, Long> {

  Optional<UssdMenuFlowEntity> findByCodeAndVersion(String code, Integer version);
}

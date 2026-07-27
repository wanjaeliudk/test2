package com.dtbafrica.tradefinanceussdservice.infra.persistence.repository;

import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdSessionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdSessionRepository extends JpaRepository<UssdSessionEntity, UUID> {

  Optional<UssdSessionEntity> findBySessionId(String sessionId);
}

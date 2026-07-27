package com.dtbafrica.tradefinanceussdservice.infra.persistence.repository;

import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdMenuTransitionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdMenuTransitionRepository
    extends JpaRepository<UssdMenuTransitionEntity, Long> {

  List<UssdMenuTransitionEntity> findByNodeIdOrderByDisplayOrderAsc(Long nodeId);
}

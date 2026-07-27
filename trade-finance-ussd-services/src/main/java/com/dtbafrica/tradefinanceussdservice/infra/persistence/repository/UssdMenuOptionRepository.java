package com.dtbafrica.tradefinanceussdservice.infra.persistence.repository;

import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdMenuOptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdMenuOptionRepository extends JpaRepository<UssdMenuOptionEntity, Long> {

  List<UssdMenuOptionEntity> findByNodeIdOrderByDisplayOrderAsc(Long nodeId);
}

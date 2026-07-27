package com.dtbafrica.tradefinanceussdservice.infra.persistence.repository;

import com.dtbafrica.tradefinanceussdservice.infra.persistence.entity.UssdMenuNodeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdMenuNodeRepository extends JpaRepository<UssdMenuNodeEntity, Long> {

  List<UssdMenuNodeEntity> findByFlowIdOrderByDisplayOrderAsc(Long flowId);
}

package com.dtbafrica.tradefinanceussdservice.service.menu;

import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuFlow;
import java.util.Optional;

public interface UssdMenuCatalog {

  Optional<UssdMenuFlow> findFlow(String flowCode);

  UssdMenuFlow requireFlow(String flowCode);
}

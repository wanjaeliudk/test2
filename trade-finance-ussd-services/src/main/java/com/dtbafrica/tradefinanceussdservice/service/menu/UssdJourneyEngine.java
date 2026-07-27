package com.dtbafrica.tradefinanceussdservice.service.menu;

import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdEndRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdResponseRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdStartRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.response.UssdGatewayResponse;
import com.dtbafrica.tradefinanceussdservice.domain.model.MockCustomerProfile;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuInputType;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuNode;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuOption;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdPinMode;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionState;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdSessionStatus;
import com.dtbafrica.tradefinanceussdservice.infra.exception.ApiException;
import com.dtbafrica.tradefinanceussdservice.service.profile.ProfileLookupService;
import com.dtbafrica.tradefinanceussdservice.service.session.UssdSessionStore;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UssdJourneyEngine {

  private static final String DEFAULT_CURRENCY = "USD";
  private static final String DEFAULT_AMOUNT = "50000";
  private static final String DEFAULT_MIN_AMOUNT = "1000";
  private static final String DEFAULT_MAX_AMOUNT = "50000";

  private final UssdSessionStore sessionStore;
  private final UssdMenuCatalog menuCatalog;
  private final ProfileLookupService profileLookupService;
  private final UssdTemplateRenderer renderer;

  public UssdGatewayResponse start(String sessionId, UssdStartRequest request) {
    MockCustomerProfile profile = profileLookupService.findByMsisdn(request.msisdn()).orElse(null);
    String flowCode =
        profile == null
            ? StaticUssdMenuCatalog.FLOW_REGISTRATION_INITIAL_LOAN
            : StaticUssdMenuCatalog.FLOW_AFTER_REGISTRATION;

    UssdSessionState sessionState = new UssdSessionState();
    sessionState.setSessionId(sessionId);
    sessionState.setMsisdn(request.msisdn());
    sessionState.setFlowCode(flowCode);
    sessionState.setCurrentNodeKey(menuCatalog.requireFlow(flowCode).startNodeKey());
    sessionState.setStatus(UssdSessionStatus.ACTIVE);
    seedContext(sessionState, profile, flowCode);
    sessionStore.save(sessionState);

    return render(sessionState, null, false);
  }

  public UssdGatewayResponse respond(String sessionId, UssdResponseRequest request) {
    UssdSessionState sessionState =
        sessionStore
            .findBySessionId(sessionId)
            .orElseThrow(() -> new ApiException("USSD session not found"));

    if (sessionState.getStatus() == UssdSessionStatus.ENDED) {
      throw new ApiException("USSD session has already ended");
    }

    String input = request.text().trim();
    if ("0".equals(input) && !sessionState.getHistory().isEmpty()) {
      sessionState.setCurrentNodeKey(sessionState.getHistory().pop());
      sessionStore.save(sessionState);
      return render(sessionState, null, false);
    }

    UssdMenuNode currentNode = currentNode(sessionState);
    Optional<TransitionTarget> target = resolveTarget(sessionState, currentNode, input);
    if (target.isEmpty()) {
      return render(sessionState, currentNode.invalidInputMessage(), false);
    }

    TransitionTarget transitionTarget = target.get();
    sessionState.getHistory().push(currentNode.key());
    sessionState.setCurrentNodeKey(transitionTarget.nextNodeKey());
    transitionTarget.updatedVariables().forEach(sessionState.getVariables()::put);
    sessionStore.save(sessionState);

    return render(sessionState, null, false);
  }

  public UssdGatewayResponse end(String sessionId, UssdEndRequest request) {
    UssdSessionState sessionState =
        sessionStore
            .findBySessionId(sessionId)
            .orElseThrow(() -> new ApiException("USSD session not found"));

    sessionState.setStatus(UssdSessionStatus.ENDED);
    sessionStore.save(sessionState);
    sessionStore.delete(sessionId);
    return new UssdGatewayResponse(true, "Thank you for using DTB USSD.", 200, "");
  }

  private void seedContext(
      UssdSessionState sessionState, MockCustomerProfile profile, String flowCode) {
    sessionState.getVariables().put("name", profile == null ? "Customer" : profile.name());
    sessionState.getVariables().put("currency", DEFAULT_CURRENCY);
    sessionState.getVariables().put("amount", DEFAULT_AMOUNT);
    sessionState.getVariables().put("minAmount", DEFAULT_MIN_AMOUNT);
    sessionState.getVariables().put("maxAmount", DEFAULT_MAX_AMOUNT);
    sessionState.getVariables().put("anchor", "Anchor One");
    sessionState.getVariables().put("pin", profile == null ? "1234" : profile.pin());
    sessionState.getVariables().put("flowCode", flowCode);
  }

  private UssdMenuNode currentNode(UssdSessionState sessionState) {
    return menuCatalog
        .requireFlow(sessionState.getFlowCode())
        .node(sessionState.getCurrentNodeKey())
        .orElseThrow(() -> new ApiException("USSD menu node not found"));
  }

  private Optional<TransitionTarget> resolveTarget(
      UssdSessionState sessionState, UssdMenuNode currentNode, String input) {
    if (currentNode.inputType() == UssdMenuInputType.NONE) {
      return Optional.of(new TransitionTarget(currentNode.nextNodeKey(), Map.of()));
    }

    if (currentNode.inputType() == UssdMenuInputType.MENU_OPTION) {
      return currentNode.options().stream()
          .filter(option -> option.inputValue().equals(input))
          .map(option -> new TransitionTarget(option.nextNodeKey(), option.contextUpdates()))
          .findFirst();
    }

    if (currentNode.inputType() == UssdMenuInputType.PIN) {
      if (!input.matches("\\d{4}")) {
        return Optional.empty();
      }
      if (currentNode.pinMode() == UssdPinMode.VERIFY_EXISTING
          && !input.equals(sessionState.getVariables().get("pin"))) {
        return Optional.empty();
      }
      return Optional.of(
          new TransitionTarget(
              currentNode.nextNodeKey(), Map.of(currentNode.variableKey(), input)));
    }

    if (currentNode.inputType() == UssdMenuInputType.AMOUNT) {
      if (!input.matches("\\d+(\\.\\d{1,2})?")) {
        return Optional.empty();
      }
      BigDecimal amount = new BigDecimal(input);
      BigDecimal minAmount = new BigDecimal(sessionState.getVariables().get("minAmount"));
      BigDecimal maxAmount = new BigDecimal(sessionState.getVariables().get("maxAmount"));
      if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
        return Optional.empty();
      }
      return Optional.of(
          new TransitionTarget(
              currentNode.nextNodeKey(), Map.of(currentNode.variableKey(), input)));
    }

    if (currentNode.inputType() == UssdMenuInputType.FREE_TEXT) {
      if (!StringUtils.hasText(input)) {
        return Optional.empty();
      }
      return Optional.of(
          new TransitionTarget(
              currentNode.nextNodeKey(), Map.of(currentNode.variableKey(), input)));
    }

    return Optional.empty();
  }

  private UssdGatewayResponse render(
      UssdSessionState sessionState, String errorMessage, boolean forceClose) {
    UssdMenuNode currentNode = currentNode(sessionState);
    String message = renderer.render(currentNode.promptTemplate(), sessionState.getVariables());
    if (currentNode.inputType() == UssdMenuInputType.MENU_OPTION
        && !currentNode.options().isEmpty()) {
      message = message + "\n" + renderOptions(currentNode.options(), sessionState.getVariables());
    }
    if (StringUtils.hasText(errorMessage)) {
      message = errorMessage + "\n" + message;
    }
    boolean shouldClose = forceClose || currentNode.closeSession() || currentNode.terminal();
    return new UssdGatewayResponse(shouldClose, message, 200, "");
  }

  private String renderOptions(List<UssdMenuOption> options, Map<String, String> variables) {
    return options.stream()
        .sorted(Comparator.comparing(UssdMenuOption::inputValue))
        .map(option -> option.inputValue() + ". " + renderer.render(option.label(), variables))
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
  }

  private static final class TransitionTarget {

    private final String nextNodeKey;
    private final Map<String, String> updatedVariables;

    private TransitionTarget(String nextNodeKey, Map<String, String> updatedVariables) {
      this.nextNodeKey = nextNodeKey;
      this.updatedVariables = updatedVariables;
    }

    private String nextNodeKey() {
      return nextNodeKey;
    }

    private Map<String, String> updatedVariables() {
      return updatedVariables;
    }
  }
}

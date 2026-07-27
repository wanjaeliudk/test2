package com.dtbafrica.tradefinanceussdservice.service.menu;

import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuFlow;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuInputType;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuNode;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdMenuOption;
import com.dtbafrica.tradefinanceussdservice.domain.model.UssdPinMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StaticUssdMenuCatalog implements UssdMenuCatalog {

  public static final String FLOW_AFTER_REGISTRATION = "journey-after-registration";
  public static final String FLOW_REGISTRATION_INITIAL_LOAN =
      "registration-initial-loan-application";

  private final Map<String, UssdMenuFlow> flows =
      Map.of(
          FLOW_AFTER_REGISTRATION, afterRegistrationFlow(),
          FLOW_REGISTRATION_INITIAL_LOAN, registrationFlow());

  @Override
  public Optional<UssdMenuFlow> findFlow(String flowCode) {
    return Optional.ofNullable(flows.get(flowCode));
  }

  @Override
  public UssdMenuFlow requireFlow(String flowCode) {
    return findFlow(flowCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown flow: " + flowCode));
  }

  private UssdMenuFlow afterRegistrationFlow() {
    Map<String, UssdMenuNode> nodes = new LinkedHashMap<>();
    nodes.put(
        "after-reg-pin",
        new UssdMenuNode(
            "after-reg-pin",
            "Hi {{name}}. Please enter your PIN to proceed.",
            UssdMenuInputType.PIN,
            UssdPinMode.VERIFY_EXISTING,
            "pin",
            "after-reg-home",
            false,
            false,
            null,
            "Invalid PIN. Please try again."));
    nodes.put(
        "after-reg-home",
        new UssdMenuNode(
            "after-reg-home",
            "Hi {{name}}, welcome to DTB.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Select anchor", "after-reg-anchor-list", Map.of()),
                new UssdMenuOption("2", "My offers", "after-reg-offer-summary", Map.of()),
                new UssdMenuOption("3", "Exit", "exit", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "after-reg-anchor-list",
        new UssdMenuNode(
            "after-reg-anchor-list",
            "Select an anchor:",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption(
                    "1",
                    "Anchor One",
                    "after-reg-anchor-one-offer",
                    Map.of("anchor", "Anchor One", "amount", "50000")),
                new UssdMenuOption(
                    "2",
                    "Anchor Two",
                    "after-reg-anchor-two-offer",
                    Map.of("anchor", "Anchor Two", "amount", "30000")),
                new UssdMenuOption("0", "Back", "after-reg-home", Map.of())),
            "Please select a valid anchor."));
    nodes.put(
        "after-reg-anchor-one-offer",
        new UssdMenuNode(
            "after-reg-anchor-one-offer",
            "You qualify for up to {{currency}} {{amount}} from Anchor One.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Apply for loan", "after-reg-amount", Map.of()),
                new UssdMenuOption("0", "Back", "after-reg-anchor-list", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "after-reg-anchor-two-offer",
        new UssdMenuNode(
            "after-reg-anchor-two-offer",
            "You qualify for up to {{currency}} {{amount}} from Anchor Two.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Apply for loan", "after-reg-amount", Map.of()),
                new UssdMenuOption("0", "Back", "after-reg-anchor-list", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "after-reg-offer-summary",
        new UssdMenuNode(
            "after-reg-offer-summary",
            "You qualify for up to {{currency}} {{amount}} from {{anchor}}.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Continue to loan", "after-reg-amount", Map.of()),
                new UssdMenuOption("0", "Back", "after-reg-home", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "after-reg-amount",
        new UssdMenuNode(
            "after-reg-amount",
            "The amount must be between {{currency}} {{minAmount}} and {{currency}}"
                + " {{maxAmount}}.\n"
                + "Enter the amount:",
            UssdMenuInputType.AMOUNT,
            null,
            "amount",
            "after-reg-confirm",
            false,
            false,
            null,
            "Enter an amount within the allowed range."));
    nodes.put(
        "after-reg-confirm",
        new UssdMenuNode(
            "after-reg-confirm",
            "Confirm application for {{currency}} {{amount}} from {{anchor}}?",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Yes", "after-reg-success", Map.of()),
                new UssdMenuOption("2", "No", "after-reg-home", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "after-reg-success",
        new UssdMenuNode(
            "after-reg-success",
            "Your trade finance application has been submitted successfully.",
            UssdMenuInputType.NONE,
            null,
            null,
            null,
            true,
            true,
            null,
            ""));
    nodes.put(
        "exit",
        new UssdMenuNode(
            "exit",
            "Thank you for using DTB USSD.",
            UssdMenuInputType.NONE,
            null,
            null,
            null,
            true,
            true,
            null,
            ""));
    return new UssdMenuFlow(FLOW_AFTER_REGISTRATION, "after-reg-pin", nodes);
  }

  private UssdMenuFlow registrationFlow() {
    Map<String, UssdMenuNode> nodes = new LinkedHashMap<>();
    nodes.put(
        "reg-welcome",
        new UssdMenuNode(
            "reg-welcome",
            "We could not find your profile.\nStart registration or exit below.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Start registration", "reg-id", Map.of()),
                new UssdMenuOption("2", "Exit", "exit", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "reg-id",
        new UssdMenuNode(
            "reg-id",
            "Enter your ID number:",
            UssdMenuInputType.FREE_TEXT,
            null,
            "idNumber",
            "reg-pin",
            false,
            false,
            null,
            "Enter a valid ID number."));
    nodes.put(
        "reg-pin",
        new UssdMenuNode(
            "reg-pin",
            "Set a 4-digit PIN:",
            UssdMenuInputType.PIN,
            UssdPinMode.CAPTURE_NEW,
            "pin",
            "reg-summary",
            false,
            false,
            null,
            "Enter a valid 4-digit PIN."));
    nodes.put(
        "reg-summary",
        new UssdMenuNode(
            "reg-summary",
            "Registration successful.\nContinue to loan application or exit below.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption(
                    "1", "Continue to loan application", "reg-anchor-list", Map.of()),
                new UssdMenuOption("2", "Exit", "exit", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "reg-anchor-list",
        new UssdMenuNode(
            "reg-anchor-list",
            "Select an anchor:",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption(
                    "1",
                    "Anchor One",
                    "reg-anchor-one-offer",
                    Map.of("anchor", "Anchor One", "amount", "50000")),
                new UssdMenuOption(
                    "2",
                    "Anchor Two",
                    "reg-anchor-two-offer",
                    Map.of("anchor", "Anchor Two", "amount", "30000")),
                new UssdMenuOption("0", "Back", "reg-summary", Map.of())),
            "Please select a valid anchor."));
    nodes.put(
        "reg-anchor-one-offer",
        new UssdMenuNode(
            "reg-anchor-one-offer",
            "You qualify for up to {{currency}} {{amount}} from Anchor One.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Apply for loan", "reg-amount", Map.of()),
                new UssdMenuOption("0", "Back", "reg-anchor-list", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "reg-anchor-two-offer",
        new UssdMenuNode(
            "reg-anchor-two-offer",
            "You qualify for up to {{currency}} {{amount}} from Anchor Two.",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Apply for loan", "reg-amount", Map.of()),
                new UssdMenuOption("0", "Back", "reg-anchor-list", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "reg-amount",
        new UssdMenuNode(
            "reg-amount",
            "The amount must be between {{currency}} {{minAmount}} and {{currency}}"
                + " {{maxAmount}}.\n"
                + "Enter the amount:",
            UssdMenuInputType.AMOUNT,
            null,
            "amount",
            "reg-confirm",
            false,
            false,
            null,
            "Enter an amount within the allowed range."));
    nodes.put(
        "reg-confirm",
        new UssdMenuNode(
            "reg-confirm",
            "Confirm application for {{currency}} {{amount}} from {{anchor}}?",
            UssdMenuInputType.MENU_OPTION,
            null,
            null,
            null,
            false,
            false,
            listOf(
                new UssdMenuOption("1", "Yes", "reg-success", Map.of()),
                new UssdMenuOption("2", "No", "reg-anchor-list", Map.of())),
            "Please select a valid option."));
    nodes.put(
        "reg-success",
        new UssdMenuNode(
            "reg-success",
            "Your registration and loan application have been submitted successfully.",
            UssdMenuInputType.NONE,
            null,
            null,
            null,
            true,
            true,
            null,
            ""));
    nodes.put(
        "exit",
        new UssdMenuNode(
            "exit",
            "Thank you for using DTB USSD.",
            UssdMenuInputType.NONE,
            null,
            null,
            null,
            true,
            true,
            null,
            ""));
    return new UssdMenuFlow(FLOW_REGISTRATION_INITIAL_LOAN, "reg-welcome", nodes);
  }

  @SafeVarargs
  private static <T> java.util.List<T> listOf(T... values) {
    return java.util.List.of(values);
  }
}

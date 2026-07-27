package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.domain.*;
import com.dtbafrica.scf.ussd.engine.UssdEngine;
import com.dtbafrica.scf.ussd.engine.UssdResponse;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Walks real journeys through the engine to check the tree is wired end to end. */
class EngineFlowTest {

    private static final String SESSION = "13cc8b28afb86c69766531";
    private static final String MSISDN = "254722000000";

    private AnnotationConfigApplicationContext context;
    private UssdEngine engine;
    private FakeProfileClient profile;
    private FakeTradeFinanceClient tradeFinance;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestWiring.class);
        engine = context.getBean(UssdEngine.class);
        profile = (FakeProfileClient) context.getBean(com.dtbafrica.scf.ussd.client.ProfileClient.class);
        tradeFinance = (FakeTradeFinanceClient)
                context.getBean(com.dtbafrica.scf.ussd.client.TradeFinanceClient.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void registeredUserBorrowsEndToEnd() {
        UssdResponse login = engine.start(SESSION, MSISDN);
        assertThat(login.ussdMenu()).contains("Hi Kamau").contains("Forgot PIN");
        assertThat(login.shouldClose()).isFalse();

        assertThat(engine.respond(SESSION, "3456").ussdMenu())
                .contains("select an anchor").contains("1. Kabianga");

        assertThat(engine.respond(SESSION, "1").ussdMenu())
                .contains("You qualify for a loan of upto KES 45,789 from Kabianga");

        assertThat(engine.respond(SESSION, "1").ussdMenu())
                .contains("How much do you want borrow?");

        assertThat(engine.respond(SESSION, "20,000").ussdMenu())
                .contains("authorise loan application");

        UssdResponse done = engine.respond(SESSION, "3456");
        assertThat(done.ussdMenu()).contains("has been received and is being processed");

        assertThat(tradeFinance.submissions).hasSize(1);
        FakeTradeFinanceClient.Submission submitted = tradeFinance.submissions.get(0);
        assertThat(submitted.application().amount()).isEqualByComparingTo("20000");
        assertThat(submitted.application().anchorId()).isEqualTo("A1");
        assertThat(submitted.idempotencyKey()).isNotBlank();
    }

    @Test
    void backFromAValidationErrorReturnsToTheInputScreen() {
        engine.start(SESSION, MSISDN);
        engine.respond(SESSION, "3456");   // anchor list
        engine.respond(SESSION, "1");      // qualify
        engine.respond(SESSION, "1");      // amount prompt

        assertThat(engine.respond(SESSION, "99999999").ussdMenu())
                .contains("must be between KES 100 and KES 45,789");

        // The error was reached with Goto, so the stack holds the amount screen.
        assertThat(engine.respond(SESSION, "0").ussdMenu())
                .contains("How much do you want borrow?");
    }

    @Test
    void amountBelowTheFloorIsRejected() {
        engine.start(SESSION, MSISDN);
        engine.respond(SESSION, "3456");
        engine.respond(SESSION, "1");
        engine.respond(SESSION, "1");

        assertThat(engine.respond(SESSION, "50").ussdMenu())
                .contains("must be between KES 100 and KES 45,789");
        assertThat(tradeFinance.submissions).isEmpty();
    }

    @Test
    void wrongPinAtAuthorisationDoesNotSubmit() {
        profile.pinResult = PinResult.OK;
        engine.start(SESSION, MSISDN);
        engine.respond(SESSION, "3456");
        engine.respond(SESSION, "1");
        engine.respond(SESSION, "1");
        engine.respond(SESSION, "20000");

        profile.pinResult = PinResult.WRONG;
        assertThat(engine.respond(SESSION, "0000").ussdMenu())
                .contains("Wrong PIN, please try again");
        assertThat(tradeFinance.submissions).isEmpty();
    }

    @Test
    void lockedPinRoutesToTheLockoutScreen() {
        profile.pinResult = PinResult.LOCKED;
        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "3456").ussdMenu()).contains("locked");
    }

    @Test
    void eligibleSupplierIsSentAnOtpAndTheSessionCloses() {
        profile.snapshot = new ProfileSnapshot(
                "SUP-1", null, RegistrationStatus.ELIGIBLE, null);

        UssdResponse welcome = engine.start(SESSION, MSISDN);
        assertThat(welcome.ussdMenu()).contains("Hello and welcome to DTB Agriloans");
        // Kiswahili is deferred, so the option is not offered.
        assertThat(welcome.ussdMenu()).doesNotContain("Kiswahili");

        UssdResponse otpSent = engine.respond(SESSION, "1");
        assertThat(otpSent.ussdMenu()).contains("An OTP has been sent to you");
        assertThat(otpSent.shouldClose()).isTrue();
        assertThat(profile.otpRequests).isEqualTo(1);
    }

    @Test
    void redialWithAnOutstandingOtpGoesStraightToEntry() {
        // The two-session shape: without OTP_PENDING the redial would re-issue forever.
        profile.snapshot = new ProfileSnapshot(
                "SUP-1", null, RegistrationStatus.OTP_PENDING, null);

        assertThat(engine.start(SESSION, MSISDN).ussdMenu())
                .contains("Please enter the OTP that was sent to you");
        assertThat(profile.otpRequests).isZero();
    }

    @Test
    void otpThenPinSetupCompletesRegistration() {
        profile.snapshot = new ProfileSnapshot(
                "SUP-1", null, RegistrationStatus.OTP_PENDING, null);

        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "2424").ussdMenu()).contains("Set a new PIN");

        assertThat(engine.respond(SESSION, "34").ussdMenu())
                .contains("PIN should be 4 digits");
        assertThat(engine.respond(SESSION, "0").ussdMenu()).contains("Set a new PIN");

        assertThat(engine.respond(SESSION, "3456").ussdMenu()).contains("Confirm your new PIN");
        assertThat(engine.respond(SESSION, "9999").ussdMenu()).contains("PINs don't match");
        assertThat(engine.respond(SESSION, "0").ussdMenu()).contains("Confirm your new PIN");

        assertThat(engine.respond(SESSION, "3456").ussdMenu())
                .contains("You're good to go!").contains("1. Kabianga");
        assertThat(profile.pinSets).isEqualTo(1);
    }

    @Test
    void expiredOtpOffersAResend() {
        profile.snapshot = new ProfileSnapshot(
                "SUP-1", null, RegistrationStatus.OTP_PENDING, null);
        profile.otpResult = OtpResult.EXPIRED;

        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "2424").ussdMenu()).contains("has expired");

        UssdResponse resent = engine.respond(SESSION, "1");
        assertThat(resent.shouldClose()).isTrue();
        assertThat(profile.otpRequests).isEqualTo(1);
    }

    @Test
    void supplierWithoutADtbAccountCanExit() {
        profile.snapshot = new ProfileSnapshot(
                null, null, RegistrationStatus.NO_DTB_ACCOUNT, null);

        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "1").ussdMenu()).contains("you need a DTB account");

        // "0" here means Exit, not Back — this node overrides standard navigation.
        UssdResponse exit = engine.respond(SESSION, "0");
        assertThat(exit.shouldClose()).isTrue();
    }

    @Test
    void unknownNumberReachesSupportDetails() {
        profile.snapshot = new ProfileSnapshot(
                null, null, RegistrationStatus.NOT_A_SUPPLIER, null);

        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "1").ussdMenu()).contains("You do not qualify");
        assertThat(engine.respond(SESSION, "1").ussdMenu())
                .contains("contactcentre@dtbafrica.com");
    }

    @Test
    void forgotPinStartsAResetAndClosesTheSession() {
        UssdResponse login = engine.start(SESSION, MSISDN);
        assertThat(login.ussdMenu()).contains("Forgot PIN");

        UssdResponse reset = engine.respond(SESSION, "1");
        assertThat(reset.shouldClose()).isTrue();
        assertThat(profile.pinResets).isEqualTo(1);
    }

    @Test
    void anchorListPaginates() {
        tradeFinance.anchors = List.of(
                new Anchor("A1", "Kabianga"),
                new Anchor("A2", "Nandi Hills"),
                new Anchor("A3", "Kericho"),
                new Anchor("A4", "Sotik Valley"));

        engine.start(SESSION, MSISDN);
        String firstPage = engine.respond(SESSION, "3456").ussdMenu();
        assertThat(firstPage).contains("Kabianga").contains("99. Next").doesNotContain("Sotik");

        String secondPage = engine.respond(SESSION, "99").ussdMenu();
        assertThat(secondPage).contains("Sotik Valley").doesNotContain("99. Next");

        // Paging used Replace, so selecting still resolves against the visible page.
        assertThat(engine.respond(SESSION, "1").ussdMenu()).contains("from Sotik Valley");
    }

    @Test
    void emptyAnchorListDoesNotDeadEnd() {
        tradeFinance.anchors = List.of();
        engine.start(SESSION, MSISDN);
        assertThat(engine.respond(SESSION, "3456").ussdMenu()).contains("no active anchors");
    }

    @Test
    void aProfileFailureRendersTheFallbackRatherThanErroring() {
        profile.failResolveWith = new IllegalStateException("profile down");
        assertThat(engine.start(SESSION, MSISDN).ussdMenu())
                .contains("cannot process this right now");
    }

    @Test
    void mainMenuFromTheSubmittedScreenReturnsToAnchorSelect() {
        engine.start(SESSION, MSISDN);
        engine.respond(SESSION, "3456");
        engine.respond(SESSION, "1");
        engine.respond(SESSION, "1");
        engine.respond(SESSION, "20000");
        engine.respond(SESSION, "3456");

        assertThat(engine.respond(SESSION, "00").ussdMenu()).contains("select an anchor");
    }

    @Test
    void endIsIdempotent() {
        engine.start(SESSION, MSISDN);
        engine.end(SESSION, 600, "timeout");
        engine.end(SESSION, 600, "timeout");   // must not throw
    }
}

package com.dtbafrica.scf.ussd.menu;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The payoff of keeping render pure: no mocks, no running services, no HTTP.
 */
class ScreenBudgetTest {

    private static AnnotationConfigApplicationContext context;
    private static MenuRegistry registry;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestWiring.class);
        registry = context.getBean(MenuRegistry.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @ParameterizedTest
    @EnumSource(NodeId.class)
    void everyScreenFitsTheUssdBudget(NodeId id) {
        String text = registry.get(id).render(WorstCase.context());
        assertThat(Gsm7.encodedLength(text))
                .as("%s renders %d septets:%n%s", id, Gsm7.encodedLength(text), text)
                .isLessThanOrEqualTo(Gsm7.SCREEN_LIMIT);
    }

    @ParameterizedTest
    @EnumSource(NodeId.class)
    void everyScreenIsGsm7Encodable(NodeId id) {
        String text = registry.get(id).render(WorstCase.context());
        assertThat(Gsm7.unsupportedChars(text))
                .as("%s contains characters GSM-7 cannot encode", id)
                .isEmpty();
    }

    @Test
    void anchorSelectFitsOnTheJustRegisteredBranch() {
        String text = registry.get(NodeId.ANCHOR_SELECT).render(WorstCase.justRegisteredContext());
        assertThat(Gsm7.encodedLength(text)).isLessThanOrEqualTo(Gsm7.SCREEN_LIMIT);
    }

    @Test
    void anchorSelectPaginatesWhenTheListOverflowsOnePage() {
        String text = registry.get(NodeId.ANCHOR_SELECT).render(WorstCase.context());
        assertThat(text).contains("99. Next");
    }

    @Test
    void longNamesAndAnchorsAreClippedNotOverflowed() {
        String text = registry.get(NodeId.PIN_LOGIN).render(WorstCase.context());
        assertThat(text).doesNotContain("Wanjiku-Kamau Njoroge");
        assertThat(Gsm7.encodedLength(text)).isLessThanOrEqualTo(Gsm7.SCREEN_LIMIT);
    }
}

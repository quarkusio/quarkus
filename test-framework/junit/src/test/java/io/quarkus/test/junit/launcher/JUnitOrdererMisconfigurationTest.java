package io.quarkus.test.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.config.QuarkusClassOrderer;
import io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer;

/**
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57084">quarkusio/quarkus#57084</a>
 */
class JUnitOrdererMisconfigurationTest {

    private static final String SECONDARY_ORDERER_PROPERTY = "junit.quarkus.orderer.secondary-orderer";

    private static final String SUREFIRE_REVERSE_ORDERER = "org.apache.maven.surefire.junitplatform.ReverseOrdering$ReverseClassOrder";

    @Test
    void quarkusOrdererIsAccepted() {
        assertThat(
                JUnitOrdererMisconfiguration.failureMessage(Optional.of(QuarkusTestProfileAwareClassOrderer.class.getName())))
                .isEmpty();
    }

    @Test
    @SuppressWarnings("removal")
    void legacyQuarkusOrdererIsAccepted() {
        assertThat(JUnitOrdererMisconfiguration.failureMessage(Optional.of(QuarkusClassOrderer.class.getName()))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassOrderer.ClassName.class, ClassOrderer.Random.class })
    void junitOrdererInjectedBySurefireRunOrderPointsAtRunOrder(Class<? extends ClassOrderer> ordererClass) {
        assertPointsAtRunOrder(ordererClass.getName());
    }

    @Test
    void surefireReverseOrdererInjectedByRunOrderPointsAtRunOrder() {
        assertPointsAtRunOrder(SUREFIRE_REVERSE_ORDERER);
    }

    private static void assertPointsAtRunOrder(String orderer) {
        String message = JUnitOrdererMisconfiguration.failureMessage(Optional.of(orderer)).orElseThrow();

        assertThat(message)
                .contains(orderer)
                .contains("runOrder")
                .contains("configurationParameters")
                .contains("junit.jupiter.testclass.order.default=" + QuarkusTestProfileAwareClassOrderer.class.getName())
                .contains("quarkus.test.class-orderer")
                .contains("junit-platform.properties")
                .doesNotContain(SECONDARY_ORDERER_PROPERTY + "=");
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassOrderer.ClassName.class, ClassOrderer.Random.class })
    void junitOrdererCanBeUsedAsSecondaryOrderer(Class<? extends ClassOrderer> ordererClass) {
        String message = JUnitOrdererMisconfiguration.failureMessage(Optional.of(ordererClass.getName())).orElseThrow();

        assertThat(message).contains("quarkus.test.class-orderer=" + ordererClass.getName());
    }

    @Test
    void surefireOnlyOrdererIsNotSuggestedAsSecondaryOrderer() {
        String message = JUnitOrdererMisconfiguration.failureMessage(Optional.of(SUREFIRE_REVERSE_ORDERER)).orElseThrow();

        assertThat(message).doesNotContain("quarkus.test.class-orderer=" + SUREFIRE_REVERSE_ORDERER);
    }

    @Test
    void customOrdererIsSuggestedAsSecondaryOrderer() {
        String orderer = CustomOrderer.class.getName();

        String message = JUnitOrdererMisconfiguration.failureMessage(Optional.of(orderer)).orElseThrow();

        assertThat(message)
                .contains("quarkus.test.class-orderer=" + orderer)
                .contains("junit-platform.properties");
    }

    @Test
    void missingOrdererExplainsTheShadowedJunitPlatformProperties() {
        String message = JUnitOrdererMisconfiguration.failureMessage(Optional.empty()).orElseThrow();

        assertThat(message)
                .contains("junit-platform.properties")
                .contains("junit.jupiter.testclass.order.default=" + QuarkusTestProfileAwareClassOrderer.class.getName());
    }

    static class CustomOrderer implements ClassOrderer {
        @Override
        public void orderClasses(ClassOrdererContext context) {
        }
    }
}

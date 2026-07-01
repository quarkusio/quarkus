package io.quarkus.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

class ReproducibilityOnlyConditionTest {

    private static final String REPRODUCIBILITY_CHECK_PROPERTY_NAME = "quarkus-internal.test.reproducibility-check";

    private final ReproducibilityOnlyCondition condition = new ReproducibilityOnlyCondition();

    @AfterEach
    void clearProperty() {
        System.clearProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME);
    }

    @Test
    void enablesAllTestsWhenReproducibilityCheckIsNotActive() {
        ConditionEvaluationResult result = condition.evaluateExecutionCondition(contextFor(PlainTest.class));

        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void disablesPlainTestsWhenReproducibilityCheckIsActive() {
        System.setProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME, "5");

        ConditionEvaluationResult result = condition.evaluateExecutionCondition(contextFor(PlainTest.class));

        assertThat(result.isDisabled()).isTrue();
    }

    @Test
    void enablesTestsWithReproducibilityCapableExtensionFieldsWhenReproducibilityCheckIsActive() {
        System.setProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME, "5");

        ConditionEvaluationResult result = condition
                .evaluateExecutionCondition(contextFor(ReproducibilityExtensionFieldTest.class));

        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void enablesTestsWithReproducibilityCapableExtensionSubclassFieldsWhenReproducibilityCheckIsActive() {
        System.setProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME, "5");

        ConditionEvaluationResult result = condition
                .evaluateExecutionCondition(contextFor(ReproducibilityExtensionSubclassFieldTest.class));

        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void enablesNestedTestsWhenEnclosingClassHasQuarkusExtensionField() {
        System.setProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME, "5");

        ConditionEvaluationResult result = condition.evaluateExecutionCondition(contextFor(EnclosingTest.NestedTest.class));

        assertThat(result.isDisabled()).isFalse();
    }

    private static ExtensionContext contextFor(Class<?> testClass) {
        return (ExtensionContext) Proxy.newProxyInstance(ReproducibilityOnlyConditionTest.class.getClassLoader(),
                new Class<?>[] { ExtensionContext.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getTestClass")) {
                        return Optional.of(testClass);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    static class PlainTest {
    }

    static class ReproducibilityExtension implements ReproducibilityCapableTestExtension {
    }

    static class ReproducibilityExtensionSubclass extends ReproducibilityExtension {
    }

    static class ReproducibilityExtensionFieldTest {
        @SuppressWarnings("unused")
        static final ReproducibilityExtension test = new ReproducibilityExtension();
    }

    static class ReproducibilityExtensionSubclassFieldTest {
        @SuppressWarnings("unused")
        static final ReproducibilityExtensionSubclass test = new ReproducibilityExtensionSubclass();
    }

    static class EnclosingTest {
        @SuppressWarnings("unused")
        static final ReproducibilityExtension test = new ReproducibilityExtension();

        static class NestedTest {
        }
    }
}

package io.quarkus.test.junit.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

final class DisabledIfConfigRepeatedTest {

    private final DisabledIfConfigCondition condition = new DisabledIfConfigCondition();

    @DisabledIfConfig(named = "disabled.if.config.repeated.prop1", matches = "one")
    @DisabledIfConfig(named = "disabled.if.config.repeated.prop2", matches = "no-match")
    private static class FirstMatches {
    }

    @DisabledIfConfig(named = "disabled.if.config.repeated.prop1", matches = "no-match")
    @DisabledIfConfig(named = "disabled.if.config.repeated.prop2", matches = "two")
    private static class SecondMatches {
    }

    @DisabledIfConfig(named = "disabled.if.config.repeated.prop1", matches = "wrong")
    @DisabledIfConfig(named = "disabled.if.config.repeated.prop2", matches = "also-wrong")
    private static class NoneMatch {
    }

    @Test
    void disabledWhenFirstConditionMatches() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(FirstMatches.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue());
    }

    @Test
    void disabledWhenSecondConditionMatches() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(SecondMatches.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue());
    }

    @Test
    void enabledWhenNoConditionsMatch() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(NoneMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse());
    }

    private static ExtensionContext extensionContextFor(Class<?> annotatedElement) {
        return (ExtensionContext) Proxy.newProxyInstance(
                ExtensionContext.class.getClassLoader(),
                new Class<?>[] { ExtensionContext.class },
                (proxy, method, args) -> {
                    if ("getElement".equals(method.getName())) {
                        return Optional.of(annotatedElement);
                    }
                    return null;
                });
    }
}

package io.quarkus.test.junit.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

final class EnabledIfConfigRepeatedTest {

    private final EnabledIfConfigCondition condition = new EnabledIfConfigCondition();

    @EnabledIfConfig(named = "enabled.if.config.repeated.prop1", matches = "alpha")
    @EnabledIfConfig(named = "enabled.if.config.repeated.prop2", matches = "beta")
    private static class AllMatch {
    }

    @EnabledIfConfig(named = "enabled.if.config.repeated.prop1", matches = "alpha")
    @EnabledIfConfig(named = "enabled.if.config.repeated.prop2", matches = "wrong")
    private static class SecondDoesNotMatch {
    }

    @EnabledIfConfig(named = "enabled.if.config.repeated.prop1", matches = "wrong")
    @EnabledIfConfig(named = "enabled.if.config.repeated.prop2", matches = "beta")
    private static class FirstDoesNotMatch {
    }

    @Test
    void enabledWhenAllConditionsMatch() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(AllMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse());
    }

    @Test
    void disabledWhenOneConditionDoesNotMatch() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(SecondDoesNotMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue());
    }

    @Test
    void disabledWhenFirstConditionDoesNotMatch() {
        assertThat(condition.evaluateExecutionCondition(extensionContextFor(FirstDoesNotMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue());
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

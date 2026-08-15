package io.quarkus.test.junit.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

final class EnabledIfConfigConditionTest {

    private final EnabledIfConfigCondition condition = new EnabledIfConfigCondition();

    @EnabledIfConfig(named = "enabled.if.config.test.property", matches = "expected-value")
    private static class ExactMatch {
    }

    @EnabledIfConfig(named = "enabled.if.config.test.property", matches = "wrong-value")
    private static class NoMatch {
    }

    @EnabledIfConfig(named = "enabled.if.config.nonexistent.property", matches = ".*")
    private static class MissingProperty {
    }

    @EnabledIfConfig(named = "enabled.if.config.test.property", matches = "expected.*")
    private static class RegexMatch {
    }

    @EnabledIfConfig(named = "enabled.if.config.test.property", matches = "^other.*$")
    private static class RegexNoMatch {
    }

    @Test
    void enabledWhenPropertyMatchesExactValue() {
        assertThat(condition.evaluate(annotationFrom(ExactMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("matches regular expression");
    }

    @Test
    void disabledWhenPropertyDoesNotMatch() {
        assertThat(condition.evaluate(annotationFrom(NoMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not match regular expression");
    }

    @Test
    void disabledWhenPropertyDoesNotExist() {
        assertThat(condition.evaluate(annotationFrom(MissingProperty.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not exist");
    }

    @Test
    void enabledWhenPropertyMatchesRegex() {
        assertThat(condition.evaluate(annotationFrom(RegexMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("matches regular expression");
    }

    @Test
    void disabledWhenPropertyDoesNotMatchRegex() {
        assertThat(condition.evaluate(annotationFrom(RegexNoMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not match regular expression");
    }

    private static EnabledIfConfig annotationFrom(Class<?> source) {
        return source.getAnnotation(EnabledIfConfig.class);
    }
}

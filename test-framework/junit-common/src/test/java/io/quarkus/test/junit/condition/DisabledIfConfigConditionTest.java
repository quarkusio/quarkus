package io.quarkus.test.junit.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

final class DisabledIfConfigConditionTest {

    private final DisabledIfConfigCondition condition = new DisabledIfConfigCondition();

    @DisabledIfConfig(named = "disabled.if.config.test.property", matches = "target-value")
    private static class ExactMatch {
    }

    @DisabledIfConfig(named = "disabled.if.config.test.property", matches = "other-value")
    private static class NoMatch {
    }

    @DisabledIfConfig(named = "disabled.if.config.nonexistent.property", matches = ".*")
    private static class MissingProperty {
    }

    @DisabledIfConfig(named = "disabled.if.config.test.property", matches = "target.*")
    private static class RegexMatch {
    }

    @DisabledIfConfig(named = "disabled.if.config.test.property", matches = "^other.*$")
    private static class RegexNoMatch {
    }

    @DisabledIfConfig(named = " disabled.if.config.test.property ", matches = "target-value")
    private static class WhitespaceInName {
    }

    @Test
    void disabledWhenPropertyMatchesExactValue() {
        assertThat(condition.evaluate(annotationFrom(ExactMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("matches regular expression");
    }

    @Test
    void enabledWhenPropertyDoesNotMatch() {
        assertThat(condition.evaluate(annotationFrom(NoMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not match regular expression");
    }

    @Test
    void enabledWhenPropertyDoesNotExist() {
        assertThat(condition.evaluate(annotationFrom(MissingProperty.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not exist");
    }

    @Test
    void disabledWhenPropertyMatchesRegex() {
        assertThat(condition.evaluate(annotationFrom(RegexMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("matches regular expression");
    }

    @Test
    void enabledWhenPropertyDoesNotMatchRegex() {
        assertThat(condition.evaluate(annotationFrom(RegexNoMatch.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isFalse())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("does not match regular expression");
    }

    @Test
    void disabledWhenPropertyNameHasWhitespace() {
        assertThat(condition.evaluate(annotationFrom(WhitespaceInName.class)))
                .isNotNull()
                .satisfies(result -> assertThat(result.isDisabled()).isTrue())
                .extracting(ConditionEvaluationResult::getReason)
                .asInstanceOf(OPTIONAL)
                .get(STRING)
                .contains("matches regular expression");
    }

    private static DisabledIfConfig annotationFrom(Class<?> source) {
        return source.getAnnotation(DisabledIfConfig.class);
    }
}

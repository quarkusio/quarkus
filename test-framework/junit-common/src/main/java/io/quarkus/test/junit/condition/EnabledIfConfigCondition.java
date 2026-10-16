package io.quarkus.test.junit.condition;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.platform.commons.util.Preconditions;

class EnabledIfConfigCondition extends RepeatableAnnotationCondition<EnabledIfConfig> {
    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("No @EnabledIfConfig conditions resulting in 'disabled' execution encountered");

    EnabledIfConfigCondition() {
        super(EnabledIfConfig.class);
    }

    @Override
    protected ConditionEvaluationResult evaluate(EnabledIfConfig annotation) {
        var name = annotation.named();
        var regex = annotation.matches();
        Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
        Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);

        try {
            var actual = ConfigProvider.getConfig().getValue(name, String.class);

            return actual.matches(regex)
                    ? ConditionEvaluationResult.enabled(
                            "Property [%s] with value [%s] matches regular expression [%s]".formatted(name, actual, regex))
                    : ConditionEvaluationResult.disabled("Property [%s] with value [%s] does not match regular expression [%s]"
                            .formatted(name, actual, regex), annotation.disabledReason());
        } catch (NoSuchElementException ex) {
            return ConditionEvaluationResult.disabled("Property [%s] does not exist".formatted(name),
                    annotation.disabledReason());
        }
    }

    @Override
    protected ConditionEvaluationResult getNoDisabledConditionsEncounteredResult() {
        return ENABLED;
    }
}
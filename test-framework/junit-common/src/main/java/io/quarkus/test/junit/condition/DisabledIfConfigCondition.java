package io.quarkus.test.junit.condition;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.platform.commons.util.Preconditions;

class DisabledIfConfigCondition extends RepeatableAnnotationCondition<DisabledIfConfig> {
    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
            "No @DisabledIfConfig conditions resulting in 'disabled' execution encountered");

    DisabledIfConfigCondition() {
        super(DisabledIfConfig.class);
    }

    @Override
    protected ConditionEvaluationResult evaluate(DisabledIfConfig annotation) {
        var name = annotation.named().strip();
        var regex = annotation.matches();
        Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
        Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);

        try {
            var actual = ConfigProvider.getConfig().getValue(name, String.class);

            if (actual == null) {
                return enabled("System property [%s] does not exist".formatted(name));
            }

            if (actual.matches(regex)) {
                return disabled(
                        "System property [%s] with value [%s] matches regular expression [%s]".formatted(name, actual, regex),
                        annotation.disabledReason());
            }

            return enabled("System property [%s] with value [%s] does not match regular expression [%s]".formatted(name, actual,
                    regex));
        } catch (NoSuchElementException ex) {
            return enabled("System property [%s] does not exist".formatted(name));
        }
    }

    @Override
    protected ConditionEvaluationResult getNoDisabledConditionsEncounteredResult() {
        return ENABLED;
    }
}

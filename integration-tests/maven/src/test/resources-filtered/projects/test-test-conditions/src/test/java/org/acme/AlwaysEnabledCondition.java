package org.acme;

import io.smallrye.config.Config;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AlwaysEnabledCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String os = Config.get().getValue("os.name", String.class);
        return ConditionEvaluationResult.enabled("enabled");

    }
}

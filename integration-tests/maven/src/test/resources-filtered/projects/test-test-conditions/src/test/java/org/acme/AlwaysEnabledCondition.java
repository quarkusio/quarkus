package org.acme;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AlwaysEnabledCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String os = ConfigProvider.getConfig().getValue("os.name", String.class);
        return ConditionEvaluationResult.enabled("enabled");

    }
}

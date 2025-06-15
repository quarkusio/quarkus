package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

class RedisCommandCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@RequiresCommand is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<RequiresCommand> optional = AnnotationUtils.findAnnotation(context.getElement(),
                RequiresCommand.class);

        if (optional.isPresent()) {
            String[] cmd = optional.get().value();
            List<String> commands = RedisServerExtension.getAvailableCommands().stream().map(String::toLowerCase)
                    .collect(Collectors.toList());
            for (String c : cmd) {
                if (!commands.contains(c.toLowerCase())) {
                    return disabled("Disabled, Redis command " + c + " not available.");
                }
            }
            return enabled("Redis commands " + String.join(", ", cmd) + " are available");
        }

        return ENABLED_BY_DEFAULT;
    }
}

package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

class Redis84OrHigherCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@RequiresRedis84OrHigher is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<RequiresRedis84OrHigher> optional = AnnotationUtils.findAnnotation(context.getElement(),
                RequiresRedis84OrHigher.class);

        if (optional.isPresent()) {
            String version = RedisServerExtension.getRedisVersion();

            return isRedis84orHigher(version) ? enabled("Redis " + version + " >= 8.4")
                    : disabled("Disabled, Redis " + version + " < 8.4");
        }

        return ENABLED_BY_DEFAULT;
    }

    public static boolean isRedis84orHigher(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        if (major > 8) {
            return true;
        }
        if (major == 8 && parts.length > 1) {
            return Integer.parseInt(parts[1]) >= 4;
        }
        return false;
    }
}

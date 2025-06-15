package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

class Redis7OrHigherCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled(
            "@RequiresRedis7OrHigher is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<RequiresRedis7OrHigher> optional = AnnotationUtils.findAnnotation(context.getElement(),
                RequiresRedis7OrHigher.class);

        if (optional.isPresent()) {
            String version = RedisServerExtension.getRedisVersion();

            return isRedis7orHigher(version) ? enabled("Redis " + version + " >= 7")
                    : disabled("Disabled, Redis " + version + " < 7");
        }

        return ENABLED_BY_DEFAULT;
    }

    public static boolean isRedis7orHigher(String version) {
        return Integer.parseInt(version.split("\\.")[0]) >= 7;
    }
}

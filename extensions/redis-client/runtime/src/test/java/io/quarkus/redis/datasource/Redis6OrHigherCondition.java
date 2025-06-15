package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

class Redis6OrHigherCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled(
            "@RequiresRedis6OrHigher is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<RequiresRedis6OrHigher> optional = AnnotationUtils.findAnnotation(context.getElement(),
                RequiresRedis6OrHigher.class);

        if (optional.isPresent()) {
            String version = RedisServerExtension.getRedisVersion();

            return isRedis6orHigher(version) ? enabled("Redis " + version + " >= 6")
                    : disabled("Disabled, Redis " + version + " < 6");
        }

        return ENABLED_BY_DEFAULT;
    }

    public static boolean isRedis6orHigher(String version) {
        return Integer.parseInt(version.split("\\.")[0]) >= 6;
    }
}

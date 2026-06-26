package io.quarkus.it.vertx.nativetransport;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.netty.channel.uring.IoUring;

public class IoUringAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            return ConditionEvaluationResult.disabled("io_uring requires Linux");
        }

        if (IoUring.isAvailable()) {
            return ConditionEvaluationResult.enabled("io_uring is available: " + IoUring.featureString());
        }

        Throwable cause = IoUring.unavailabilityCause();
        String reason = cause != null ? cause.getMessage() : "unknown";
        return ConditionEvaluationResult.disabled("io_uring is not available: " + reason);
    }
}

package io.quarkus.smallrye.faulttolerance.runtime.devui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.arc.Arc;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FaultToleranceJsonRpcService {
    public JsonArray getGuardedMethods() {
        QuarkusFaultToleranceOperationProvider provider = Arc.container()
                .select(QuarkusFaultToleranceOperationProvider.class).get();
        List<FaultToleranceOperation> operations = new ArrayList<>(provider.getOperationCache().values());
        operations.sort(Comparator.comparing(FaultToleranceOperation::getName));

        JsonArray result = new JsonArray();
        for (FaultToleranceOperation operation : operations) {
            operation.materialize();
            result.add(convert(operation));
        }
        return result;
    }

    private JsonObject convert(FaultToleranceOperation operation) {
        JsonObject result = new JsonObject();

        result.put("beanClass", operation.getBeanClass().getName());
        result.put("method", operation.getMethodDescriptor().name);

        if (operation.hasApplyFaultTolerance()) {
            result.put(ApplyFaultTolerance.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getApplyFaultTolerance().value()));
        }
        if (operation.hasApplyGuard()) {
            result.put(ApplyGuard.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getApplyGuard().value()));
        }

        if (operation.hasAsynchronous()) {
            result.put(Asynchronous.class.getSimpleName(), new JsonObject());
        }
        if (operation.hasAsynchronousNonBlocking()) {
            result.put(AsynchronousNonBlocking.class.getSimpleName(), new JsonObject());
        }
        if (operation.hasBlocking()) {
            result.put(Blocking.class.getSimpleName(), new JsonObject());
        }
        if (operation.hasNonBlocking()) {
            result.put(NonBlocking.class.getSimpleName(), new JsonObject());
        }

        if (operation.hasBulkhead()) {
            result.put(Bulkhead.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getBulkhead().value())
                    .put("waitingTaskQueue", operation.getBulkhead().waitingTaskQueue()));
        }
        if (operation.hasCircuitBreaker()) {
            result.put(CircuitBreaker.class.getSimpleName(), new JsonObject()
                    .put("delay", operation.getCircuitBreaker().delay())
                    .put("delayUnit", operation.getCircuitBreaker().delayUnit())
                    .put("requestVolumeThreshold", operation.getCircuitBreaker().requestVolumeThreshold())
                    .put("failureRatio", operation.getCircuitBreaker().failureRatio())
                    .put("successThreshold", operation.getCircuitBreaker().successThreshold())
                    .put("failOn", convert(operation.getCircuitBreaker().failOn()))
                    .put("skipOn", convert(operation.getCircuitBreaker().skipOn())));
        }
        if (operation.hasCircuitBreakerName()) {
            result.put(CircuitBreakerName.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getCircuitBreakerName().value()));
        }
        if (operation.hasFallback()) {
            result.put(Fallback.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getFallback().value().getName())
                    .put("fallbackMethod", operation.getFallback().fallbackMethod())
                    .put("applyOn", convert(operation.getFallback().applyOn()))
                    .put("skipOn", convert(operation.getFallback().skipOn())));
        }
        if (operation.hasRateLimit()) {
            result.put(RateLimit.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getRateLimit().value())
                    .put("window", operation.getRateLimit().window())
                    .put("windowUnit", operation.getRateLimit().windowUnit())
                    .put("minSpacing", operation.getRateLimit().minSpacing())
                    .put("minSpacingUnit", operation.getRateLimit().minSpacingUnit())
                    .put("type", operation.getRateLimit().type()));
        }
        if (operation.hasRetry()) {
            result.put(Retry.class.getSimpleName(), new JsonObject()
                    .put("maxRetries", operation.getRetry().maxRetries())
                    .put("delay", operation.getRetry().delay())
                    .put("delayUnit", operation.getRetry().delayUnit())
                    .put("maxDuration", operation.getRetry().maxDuration())
                    .put("maxDurationUnit", operation.getRetry().durationUnit())
                    .put("jitter", operation.getRetry().jitter())
                    .put("jitterUnit", operation.getRetry().jitterDelayUnit())
                    .put("retryOn", convert(operation.getRetry().retryOn()))
                    .put("abortOn", convert(operation.getRetry().abortOn())));
        }
        if (operation.hasExponentialBackoff()) {
            result.put(ExponentialBackoff.class.getSimpleName(), new JsonObject()
                    .put("factor", operation.getExponentialBackoff().factor())
                    .put("maxDelay", operation.getExponentialBackoff().maxDelay())
                    .put("maxDelayUnit", operation.getExponentialBackoff().maxDelayUnit()));
        }
        if (operation.hasFibonacciBackoff()) {
            result.put(FibonacciBackoff.class.getSimpleName(), new JsonObject()
                    .put("maxDelay", operation.getFibonacciBackoff().maxDelay())
                    .put("maxDelayUnit", operation.getFibonacciBackoff().maxDelayUnit()));
        }
        if (operation.hasCustomBackoff()) {
            result.put(CustomBackoff.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getCustomBackoff().value().getName()));
        }
        if (operation.hasRetryWhen()) {
            result.put(RetryWhen.class.getSimpleName(), new JsonObject()
                    .put("result", operation.getRetryWhen().result().getName())
                    .put("exception", operation.getRetryWhen().exception().getName()));
        }
        if (operation.hasBeforeRetry()) {
            result.put(BeforeRetry.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getBeforeRetry().value().getName())
                    .put("methodName", operation.getBeforeRetry().methodName()));
        }
        if (operation.hasTimeout()) {
            result.put(Timeout.class.getSimpleName(), new JsonObject()
                    .put("value", operation.getTimeout().value())
                    .put("valueUnit", operation.getTimeout().unit()));
        }

        return result;
    }

    private static JsonArray convert(Class<?>[] classes) {
        JsonArray result = new JsonArray();
        for (Class<?> clazz : classes) {
            result.add(clazz.getName());
        }
        return result;
    }
}

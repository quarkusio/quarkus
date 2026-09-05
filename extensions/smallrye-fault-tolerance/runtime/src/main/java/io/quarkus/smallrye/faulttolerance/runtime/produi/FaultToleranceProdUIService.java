package io.quarkus.smallrye.faulttolerance.runtime.produi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
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

/**
 * Read-only view of the fault-tolerance guarded methods, shared by Dev UI and
 * Prod UI. It exposes only the guard configuration (which annotations apply and
 * their parameters) - no invocation, mutation or secrets - so it is safe to
 * serve in production. Returns plain maps/lists so no JSON library is needed on
 * the runtime classpath.
 */
public class FaultToleranceProdUIService {

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("List all fault-tolerance guarded methods and their guard configuration")
    public List<Map<String, Object>> getGuardedMethods() {
        QuarkusFaultToleranceOperationProvider provider = Arc.container()
                .select(QuarkusFaultToleranceOperationProvider.class).get();
        List<FaultToleranceOperation> operations = new ArrayList<>(provider.getOperationCache().values());
        operations.sort(Comparator.comparing(FaultToleranceOperation::getName));

        List<Map<String, Object>> result = new ArrayList<>();
        for (FaultToleranceOperation operation : operations) {
            operation.materialize();
            result.add(convert(operation));
        }
        return result;
    }

    private Map<String, Object> convert(FaultToleranceOperation operation) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("beanClass", operation.getBeanClass().getName());
        result.put("method", operation.getMethodDescriptor().name);

        if (operation.hasApplyFaultTolerance()) {
            result.put(ApplyFaultTolerance.class.getSimpleName(),
                    Map.of("value", operation.getApplyFaultTolerance().value()));
        }
        if (operation.hasApplyGuard()) {
            result.put(ApplyGuard.class.getSimpleName(),
                    Map.of("value", operation.getApplyGuard().value()));
        }

        if (operation.hasAsynchronous()) {
            result.put(Asynchronous.class.getSimpleName(), Map.of());
        }
        if (operation.hasAsynchronousNonBlocking()) {
            result.put(AsynchronousNonBlocking.class.getSimpleName(), Map.of());
        }
        if (operation.hasBlocking()) {
            result.put(Blocking.class.getSimpleName(), Map.of());
        }
        if (operation.hasNonBlocking()) {
            result.put(NonBlocking.class.getSimpleName(), Map.of());
        }

        if (operation.hasBulkhead()) {
            result.put(Bulkhead.class.getSimpleName(), mapOf(
                    "value", operation.getBulkhead().value(),
                    "waitingTaskQueue", operation.getBulkhead().waitingTaskQueue()));
        }
        if (operation.hasCircuitBreaker()) {
            result.put(CircuitBreaker.class.getSimpleName(), mapOf(
                    "delay", operation.getCircuitBreaker().delay(),
                    "delayUnit", operation.getCircuitBreaker().delayUnit(),
                    "requestVolumeThreshold", operation.getCircuitBreaker().requestVolumeThreshold(),
                    "failureRatio", operation.getCircuitBreaker().failureRatio(),
                    "successThreshold", operation.getCircuitBreaker().successThreshold(),
                    "failOn", convert(operation.getCircuitBreaker().failOn()),
                    "skipOn", convert(operation.getCircuitBreaker().skipOn())));
        }
        if (operation.hasCircuitBreakerName()) {
            result.put(CircuitBreakerName.class.getSimpleName(),
                    Map.of("value", operation.getCircuitBreakerName().value()));
        }
        if (operation.hasFallback()) {
            result.put(Fallback.class.getSimpleName(), mapOf(
                    "value", operation.getFallback().value().getName(),
                    "fallbackMethod", operation.getFallback().fallbackMethod(),
                    "applyOn", convert(operation.getFallback().applyOn()),
                    "skipOn", convert(operation.getFallback().skipOn())));
        }
        if (operation.hasRateLimit()) {
            result.put(RateLimit.class.getSimpleName(), mapOf(
                    "value", operation.getRateLimit().value(),
                    "window", operation.getRateLimit().window(),
                    "windowUnit", operation.getRateLimit().windowUnit(),
                    "minSpacing", operation.getRateLimit().minSpacing(),
                    "minSpacingUnit", operation.getRateLimit().minSpacingUnit(),
                    "type", operation.getRateLimit().type()));
        }
        if (operation.hasRetry()) {
            result.put(Retry.class.getSimpleName(), mapOf(
                    "maxRetries", operation.getRetry().maxRetries(),
                    "delay", operation.getRetry().delay(),
                    "delayUnit", operation.getRetry().delayUnit(),
                    "maxDuration", operation.getRetry().maxDuration(),
                    "maxDurationUnit", operation.getRetry().durationUnit(),
                    "jitter", operation.getRetry().jitter(),
                    "jitterUnit", operation.getRetry().jitterDelayUnit(),
                    "retryOn", convert(operation.getRetry().retryOn()),
                    "abortOn", convert(operation.getRetry().abortOn())));
        }
        if (operation.hasExponentialBackoff()) {
            result.put(ExponentialBackoff.class.getSimpleName(), mapOf(
                    "factor", operation.getExponentialBackoff().factor(),
                    "maxDelay", operation.getExponentialBackoff().maxDelay(),
                    "maxDelayUnit", operation.getExponentialBackoff().maxDelayUnit()));
        }
        if (operation.hasFibonacciBackoff()) {
            result.put(FibonacciBackoff.class.getSimpleName(), mapOf(
                    "maxDelay", operation.getFibonacciBackoff().maxDelay(),
                    "maxDelayUnit", operation.getFibonacciBackoff().maxDelayUnit()));
        }
        if (operation.hasCustomBackoff()) {
            result.put(CustomBackoff.class.getSimpleName(),
                    Map.of("value", operation.getCustomBackoff().value().getName()));
        }
        if (operation.hasRetryWhen()) {
            result.put(RetryWhen.class.getSimpleName(), mapOf(
                    "result", operation.getRetryWhen().result().getName(),
                    "exception", operation.getRetryWhen().exception().getName()));
        }
        if (operation.hasBeforeRetry()) {
            result.put(BeforeRetry.class.getSimpleName(), mapOf(
                    "value", operation.getBeforeRetry().value().getName(),
                    "methodName", operation.getBeforeRetry().methodName()));
        }
        if (operation.hasTimeout()) {
            result.put(Timeout.class.getSimpleName(), mapOf(
                    "value", operation.getTimeout().value(),
                    "valueUnit", operation.getTimeout().unit()));
        }

        return result;
    }

    private static List<String> convert(Class<?>[] classes) {
        List<String> result = new ArrayList<>(classes.length);
        for (Class<?> clazz : classes) {
            result.add(clazz.getName());
        }
        return result;
    }

    private static Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}

package io.quarkus.smallrye.faulttolerance.deployment;

import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.jandex.DotName;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.FaultToleranceInterceptor;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;

public final class DotNames {
    public static final DotName OBJECT = DotName.createSimple(Object.class);

    public static final DotName FALLBACK_HANDLER = DotName.createSimple(FallbackHandler.class);

    public static final DotName FAULT_TOLERANCE_INTERCEPTOR = DotName.createSimple(FaultToleranceInterceptor.class);

    // ---
    // fault tolerance annotations

    public static final DotName APPLY_FAULT_TOLERANCE = DotName.createSimple(ApplyFaultTolerance.class);

    public static final DotName ASYNCHRONOUS = DotName.createSimple(Asynchronous.class);
    public static final DotName ASYNCHRONOUS_NON_BLOCKING = DotName.createSimple(AsynchronousNonBlocking.class);
    public static final DotName BLOCKING = DotName.createSimple(Blocking.class);
    public static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class);

    public static final DotName BULKHEAD = DotName.createSimple(Bulkhead.class);
    public static final DotName CIRCUIT_BREAKER = DotName.createSimple(CircuitBreaker.class);
    public static final DotName CIRCUIT_BREAKER_NAME = DotName.createSimple(CircuitBreakerName.class);
    public static final DotName FALLBACK = DotName.createSimple(Fallback.class);
    public static final DotName RATE_LIMIT = DotName.createSimple(RateLimit.class);
    public static final DotName RETRY = DotName.createSimple(Retry.class);
    public static final DotName TIMEOUT = DotName.createSimple(Timeout.class);

    public static final DotName EXPONENTIAL_BACKOFF = DotName.createSimple(ExponentialBackoff.class);
    public static final DotName FIBONACCI_BACKOFF = DotName.createSimple(FibonacciBackoff.class);
    public static final DotName CUSTOM_BACKOFF = DotName.createSimple(CustomBackoff.class);
    public static final DotName CUSTOM_BACKOFF_STRATEGY = DotName.createSimple(CustomBackoffStrategy.class);

    // certain SmallRye annotations (@CircuitBreakerName, @[Non]Blocking, @*Backoff) alone do _not_ trigger
    // the fault tolerance interceptor, only in combination with other fault tolerance annotations
    public static final Set<DotName> FT_ANNOTATIONS = Set.of(APPLY_FAULT_TOLERANCE, ASYNCHRONOUS,
            ASYNCHRONOUS_NON_BLOCKING, BULKHEAD, CIRCUIT_BREAKER, FALLBACK, RATE_LIMIT, RETRY, TIMEOUT);

    public static final Set<DotName> BACKOFF_ANNOTATIONS = Set.of(EXPONENTIAL_BACKOFF, FIBONACCI_BACKOFF, CUSTOM_BACKOFF);
}

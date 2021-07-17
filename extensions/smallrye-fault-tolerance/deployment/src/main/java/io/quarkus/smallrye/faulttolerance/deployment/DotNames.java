package io.quarkus.smallrye.faulttolerance.deployment;

import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.jandex.DotName;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

public final class DotNames {
    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static final DotName ASYNCHRONOUS = DotName.createSimple(Asynchronous.class.getName());
    public static final DotName BULKHEAD = DotName.createSimple(Bulkhead.class.getName());
    public static final DotName CIRCUIT_BREAKER = DotName.createSimple(CircuitBreaker.class.getName());
    public static final DotName FALLBACK = DotName.createSimple(Fallback.class.getName());
    public static final DotName RETRY = DotName.createSimple(Retry.class.getName());
    public static final DotName TIMEOUT = DotName.createSimple(Timeout.class.getName());

    // SmallRye annotations (@CircuitBreakerName, @[Non]Blocking, @*Backoff) alone do _not_ trigger
    // the fault tolerance interceptor, only in combination with other fault tolerance annotations
    public static final Set<DotName> FT_ANNOTATIONS = Set.of(ASYNCHRONOUS, BULKHEAD, CIRCUIT_BREAKER, FALLBACK, RETRY, TIMEOUT);

    public static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    public static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class.getName());

    public static final DotName CIRCUIT_BREAKER_NAME = DotName.createSimple(CircuitBreakerName.class.getName());

    public static final DotName EXPONENTIAL_BACKOFF = DotName.createSimple(ExponentialBackoff.class.getName());
    public static final DotName FIBONACCI_BACKOFF = DotName.createSimple(FibonacciBackoff.class.getName());
    public static final DotName CUSTOM_BACKOFF = DotName.createSimple(CustomBackoff.class.getName());
    public static final DotName CUSTOM_BACKOFF_STRATEGY = DotName.createSimple(CustomBackoffStrategy.class.getName());

    public static final Set<DotName> BACKOFF_ANNOTATIONS = Set.of(EXPONENTIAL_BACKOFF, FIBONACCI_BACKOFF, CUSTOM_BACKOFF);
}

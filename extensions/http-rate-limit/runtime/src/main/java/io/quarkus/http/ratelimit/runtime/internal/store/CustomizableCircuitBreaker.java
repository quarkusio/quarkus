package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.quarkus.http.ratelimit.RateLimitStoreGuardCustomizer;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.Guard;

/**
 * The circuit breaker builder handed to {@link RateLimitStoreGuardCustomizer}s. It delegates to the real builder, but
 * remembers the delay, which the probe and the closed failure mode follow, keeps a state change callback next to the
 * extension's own instead of replacing it, keeps the exception the extension always skips next to a customizer's
 * {@code skipOn} or {@code when} instead of letting them replace it, and refuses {@link #done()}, which only the
 * extension calls.
 */
final class CustomizableCircuitBreaker implements Guard.Builder.CircuitBreakerBuilder {

    private final Guard.Builder.CircuitBreakerBuilder delegate;
    private final Class<? extends Throwable> alwaysSkipOn;
    private Duration delay;
    private Consumer<CircuitBreakerState> onStateChange = new Consumer<>() {
        @Override
        public void accept(CircuitBreakerState state) {
        }
    };
    private Collection<Class<? extends Throwable>> skipOn = List.of();
    private Predicate<Throwable> when;

    /**
     * @param alwaysSkipOn an exception that the breaker never counts as a failure, whatever a customizer sets
     */
    CustomizableCircuitBreaker(Guard.Builder.CircuitBreakerBuilder delegate, Class<? extends Throwable> alwaysSkipOn) {
        this.delegate = delegate;
        this.alwaysSkipOn = alwaysSkipOn;
    }

    /**
     * @return the delay last set
     */
    Duration delay() {
        return delay;
    }

    /**
     * Completes the breaker with the extension's state change callback, followed by the one a customizer set, and with
     * {@code alwaysSkipOn} added to the customizer's {@code skipOn}, or excluded from its {@code when} predicate.
     * SmallRye Fault Tolerance still refuses a customizer that sets both {@code when} and {@code skipOn} or
     * {@code failOn}.
     */
    Guard.Builder complete(Consumer<CircuitBreakerState> extensionOnStateChange) {
        Consumer<CircuitBreakerState> customized = onStateChange;
        if (when != null) {
            Predicate<Throwable> customizedWhen = when;
            delegate.when(new Predicate<>() {
                @Override
                public boolean test(Throwable failure) {
                    return !causedBy(failure, alwaysSkipOn) && customizedWhen.test(failure);
                }
            });
        } else {
            List<Class<? extends Throwable>> skipped = new ArrayList<>(skipOn);
            skipped.add(alwaysSkipOn);
            delegate.skipOn(skipped);
        }
        return delegate.onStateChange(extensionOnStateChange.andThen(customized)).done();
    }

    /**
     * Whether {@code failure} or one of its causes is a {@code type}, as SmallRye Fault Tolerance matches
     * {@code skipOn}.
     */
    static boolean causedBy(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder failOn(Collection<Class<? extends Throwable>> value) {
        delegate.failOn(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder failOn(Class<? extends Throwable> value) {
        delegate.failOn(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder skipOn(Collection<Class<? extends Throwable>> value) {
        // validates the value like the real builder; the real skipOn is set in complete()
        delegate.skipOn(value);
        this.skipOn = List.copyOf(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder skipOn(Class<? extends Throwable> value) {
        delegate.skipOn(value);
        this.skipOn = List.of(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder when(Predicate<Throwable> value) {
        // validates the predicate like the real builder; the real predicate is set in complete()
        delegate.when(value);
        this.when = value;
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder delay(long value, ChronoUnit unit) {
        delegate.delay(value, unit);
        this.delay = Duration.of(value, unit);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder requestVolumeThreshold(int value) {
        delegate.requestVolumeThreshold(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder failureRatio(double value) {
        delegate.failureRatio(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder successThreshold(int value) {
        delegate.successThreshold(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder name(String value) {
        delegate.name(value);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder onStateChange(Consumer<CircuitBreakerState> callback) {
        // validates the callback like the real builder; the real callback is set in complete()
        delegate.onStateChange(callback);
        this.onStateChange = callback;
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder onSuccess(Runnable callback) {
        delegate.onSuccess(callback);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder onFailure(Runnable callback) {
        delegate.onFailure(callback);
        return this;
    }

    @Override
    public Guard.Builder.CircuitBreakerBuilder onPrevented(Runnable callback) {
        delegate.onPrevented(callback);
        return this;
    }

    @Override
    public Guard.Builder done() {
        throw new UnsupportedOperationException(
                "A RateLimitStoreGuardCustomizer must not call done(); the extension completes the circuit breaker");
    }
}

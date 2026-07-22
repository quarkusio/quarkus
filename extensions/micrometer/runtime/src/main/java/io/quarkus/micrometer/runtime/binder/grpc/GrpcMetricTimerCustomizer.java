package io.quarkus.micrometer.runtime.binder.grpc;

import java.time.Duration;
import java.util.List;
import java.util.function.UnaryOperator;

import io.micrometer.core.instrument.Timer;

/**
 * Builds Micrometer {@link Timer.Builder} customizers for gRPC metrics interceptors.
 */
public final class GrpcMetricTimerCustomizer {

    private GrpcMetricTimerCustomizer() {
    }

    /**
     * Creates a timer customizer that optionally publishes fixed SLO histogram buckets.
     *
     * @param histogram whether histogram buckets should be published
     * @param slos bucket boundaries used when {@code histogram} is {@code true}
     */
    public static UnaryOperator<Timer.Builder> create(boolean histogram, List<Duration> slos) {
        if (!histogram) {
            return UnaryOperator.identity();
        }
        Duration[] buckets = slos.toArray(Duration[]::new);
        return timer -> timer.serviceLevelObjectives(buckets);
    }
}

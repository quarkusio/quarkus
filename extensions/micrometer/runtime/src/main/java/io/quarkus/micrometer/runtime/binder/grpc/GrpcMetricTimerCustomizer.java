package io.quarkus.micrometer.runtime.binder.grpc;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import io.micrometer.core.instrument.Timer;

/**
 * Builds Micrometer {@link Timer.Builder} customizers for gRPC metrics interceptors.
 */
public final class GrpcMetricTimerCustomizer {

    private GrpcMetricTimerCustomizer() {
    }

    /**
     * Creates a timer customizer that optionally publishes Micrometer percentile histogram buckets.
     * <p>
     * When {@code histogram} is {@code true}, Micrometer's default percentile histogram is enabled
     * via {@link Timer.Builder#publishPercentileHistogram()}. Optional SLO boundaries can be
     * supplied to add extra buckets; {@link Timer.Builder#publishPercentiles(double...)} is never
     * used.
     *
     * @param histogram whether histogram buckets should be published
     * @param slos optional extra SLO bucket boundaries; empty means Micrometer defaults only
     */
    public static UnaryOperator<Timer.Builder> create(boolean histogram, Optional<List<Duration>> slos) {
        if (!histogram) {
            return UnaryOperator.identity();
        }
        return timer -> {
            Timer.Builder builder = timer.publishPercentileHistogram();
            if (slos.isPresent() && !slos.get().isEmpty()) {
                builder.serviceLevelObjectives(slos.get().toArray(Duration[]::new));
            }
            return builder;
        };
    }
}

package io.quarkus.load.shedding.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.load-shedding")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LoadSheddingRuntimeConfig {
    /**
     * Whether load shedding should be enabled.
     * Currently, this only applies to incoming HTTP requests.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The maximum number of concurrent requests allowed.
     * <p>
     * Must be at least 1; the application fails to start otherwise.
     */
    @WithDefault("1000")
    int maxLimit();

    /**
     * The {@code alpha} factor of the Vegas overload detection algorithm.
     * The algorithm grows the limit while the estimated request queue is shorter than
     * this factor times {@code log10(current limit) + 1}.
     * <p>
     * Must be at least 1; the application fails to start otherwise.
     */
    @WithDefault("3")
    int alphaFactor();

    /**
     * The {@code beta} factor of the Vegas overload detection algorithm.
     * The algorithm shrinks the limit once the estimated request queue is longer than
     * this factor times {@code log10(current limit) + 1}, and holds the limit steady in between.
     * <p>
     * Must be greater than the {@code alpha} factor; the application fails to start otherwise.
     */
    @WithDefault("6")
    int betaFactor();

    /**
     * The probe factor of the Vegas overload detection algorithm.
     * It controls how often the algorithm discards the lowest request time it has seen and
     * measures against a fresh one; the higher the factor, the rarer that is.
     * <p>
     * Must be positive; the application fails to start otherwise.
     */
    @WithDefault("30.0")
    double probeFactor();

    /**
     * The initial limit of concurrent requests allowed.
     * <p>
     * Must be at least 1 and less than or equal to {@code max-limit}; the application fails
     * to start otherwise.
     */
    @WithDefault("100")
    int initialLimit();

    /**
     * The upper bound on the {@code Retry-After} response header sent with a rejected
     * request.
     * <p>
     * The value actually sent is derived from how long the overload has already lasted,
     * so no lower bound is configurable. This is only a safety measure against telling clients
     * to stay away hours, if the overload has been going on for long.
     */
    @WithDefault("120s")
    Duration maxRetryAfter();

    /**
     * Configuration of priority load shedding.
     */
    PriorityLoadShedding priority();

    @ConfigGroup
    interface PriorityLoadShedding {
        /**
         * Whether priority load shedding should be enabled.
         */
        @WithDefault("true")
        boolean enabled();
    }
}

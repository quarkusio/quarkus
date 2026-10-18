package io.quarkus.load.shedding.runtime;

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
     */
    @WithDefault("1000")
    int maxLimit();

    /**
     * The {@code alpha} factor of the Vegas overload detection algorithm.
     */
    @WithDefault("3")
    int alphaFactor();

    /**
     * The {@code beta} factor of the Vegas overload detection algorithm.
     */
    @WithDefault("6")
    int betaFactor();

    /**
     * The probe factor of the Vegas overload detection algorithm.
     */
    @WithDefault("30.0")
    double probeFactor();

    /**
     * The initial limit of concurrent requests allowed.
     */
    @WithDefault("100")
    int initialLimit();

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

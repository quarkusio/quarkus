package io.quarkus.signals.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.signals")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface SignalsBuildTimeConfig {

    /**
     * Telemetry configuration.
     */
    Telemetry telemetry();

    interface Telemetry {

        /**
         * If collection of signal traces is enabled. When enabled, the trace context is propagated from the signal
         * emission to the receiver invocations, and a span is created for each receiver invocation.
         * <p>
         * Only applicable when the OpenTelemetry extension is present.
         */
        @WithName("traces.enabled")
        @WithDefault("true")
        boolean tracesEnabled();

        /**
         * If collection of signal metrics is enabled. When enabled, counters are registered for the number of emissions,
         * receiver invocations and failed receiver invocations.
         * <p>
         * Only applicable when the Micrometer extension is present.
         */
        @WithName("metrics.enabled")
        @WithDefault("true")
        boolean metricsEnabled();
    }
}

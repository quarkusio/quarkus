package io.quarkus.smallrye.reactivemessaging.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.messaging")
public interface ReactiveMessagingConfiguration {

    /**
     * Whether Reactive Messaging metrics are published in case a metrics extension is present
     * (default to false).
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Enables or disables the strict validation mode.
     */
    @WithDefault("false")
    boolean strict();

    /**
     * Execution mode for the Messaging signatures considered "blocking", defaults to "worker".
     * For the previous behaviour set to "event-loop".
     */
    @WithName("blocking.signatures.execution.mode")
    @WithDefault("worker")
    ExecutionMode blockingSignaturesExecutionMode();

    public enum ExecutionMode {
        EVENT_LOOP,
        WORKER,
        VIRTUAL_THREAD
    }
}

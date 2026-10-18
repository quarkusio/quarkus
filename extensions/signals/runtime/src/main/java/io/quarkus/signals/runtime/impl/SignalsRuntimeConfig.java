package io.quarkus.signals.runtime.impl;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.signals")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SignalsRuntimeConfig {

    /**
     * Receivers configuration.
     */
    Receivers receivers();

    interface Receivers {

        /**
         * The maximum number of receivers with blocking execution model that can execute concurrently.
         * <p>
         * Requests that exceed the limit are queued and executed as prior receivers complete.
         * If not set, no concurrency limit is applied.
         */
        OptionalInt blockingConcurrencyLimit();

        /**
         * The maximum number of receivers with virtual thread execution model that can execute concurrently.
         * <p>
         * Requests that exceed the limit are queued and executed as prior receivers complete.
         * If not set, no concurrency limit is applied.
         */
        OptionalInt virtualThreadConcurrencyLimit();

    }
}

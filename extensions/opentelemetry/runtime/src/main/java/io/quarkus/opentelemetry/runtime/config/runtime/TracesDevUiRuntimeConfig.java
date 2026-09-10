package io.quarkus.opentelemetry.runtime.config.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings for the dev-mode Dev UI traces buffer.
 */
@ConfigMapping(prefix = "quarkus.dev-ui.observability.traces")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TracesDevUiRuntimeConfig {

    /**
     * Maximum number of finished spans held in the in-memory ring buffer. Oldest
     * spans are evicted first.
     */
    @WithDefault("1000")
    int capacity();
}

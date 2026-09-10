package io.quarkus.opentelemetry.runtime.config.build;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time gate for the dev-mode Dev UI traces capture. Read in a {@code @BuildStep}
 * so the capture beans and Dev UI page are only registered when enabled.
 */
@ConfigMapping(prefix = "quarkus.dev-ui.observability.traces")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TracesDevUiBuildTimeConfig {

    /**
     * Whether the Dev UI Observability traces card is enabled. Dev mode only; has no
     * effect in prod/native. Changing it triggers a dev live-reload restart.
     */
    @WithDefault("true")
    boolean enabled();
}

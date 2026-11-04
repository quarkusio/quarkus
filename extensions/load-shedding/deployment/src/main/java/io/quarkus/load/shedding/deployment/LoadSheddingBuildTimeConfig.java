package io.quarkus.load.shedding.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.load-shedding")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface LoadSheddingBuildTimeConfig {
    /**
     * Whether metrics about load shedding should be published.
     * <p>
     * Only takes effect if a metrics extension is present.
     */
    @WithDefault("true")
    @WithName("metrics.enabled")
    boolean metricsEnabled();
}

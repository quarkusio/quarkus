package io.quarkus.observability.runtime.config;

import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.observability")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ObservabilityConfiguration extends ModulesConfiguration {
    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a containers when running in Dev or Test mode and when Docker is running.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Enable simplified usage of dev resources,
     * instead of full observability processing.
     * Make sure @code{enabled} is set to false.
     */
    @WithDefault("false")
    boolean devResources();

    /**
     * Do we start the dev services in parallel.
     */
    @WithDefault("false")
    boolean parallel();
}

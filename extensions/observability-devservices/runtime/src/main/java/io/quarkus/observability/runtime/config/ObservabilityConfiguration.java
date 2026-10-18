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
     * If DevServices has been explicitly enabled.
     * <p>
     * DevServices is generally enabled by default, unless there is an existing configuration present set to false.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start observability
     * containers when running in Dev mode, when Docker is running and in test mode,
     * if enabled-in-tests config is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether to start observability Dev Services in test mode.
     * <p>
     * When set to {@code false} (the default), the Grafana LGTM container will not be started
     * automatically when running tests. Set to {@code true} to enable the container in test mode.
     * <p>
     * This property has no effect in dev mode, where Dev Services are controlled solely
     * by {@code enabled}.
     */
    @WithDefault("false")
    boolean enabledInTests();

    /**
     * Enable simplified usage of dev resources,
     * instead of full observability processing.
     * No shared container discovery, no compose support, no Dev UI card and no override properties injected.
     * Make sure @code{enabled} is set to false unless you know exactly what you are doing.
     */
    @WithDefault("false")
    boolean devResources();

    /**
     * Do we start the dev services in parallel.
     */
    @WithDefault("false")
    boolean parallel();
}

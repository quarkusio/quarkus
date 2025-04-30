package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ArcDevModeConfig {

    /**
     * If set to true then the container monitors business method invocations and fired events during the development mode.
     * <p>
     * NOTE: This config property should not be changed in the development mode as it requires a full rebuild of the application
     */
    @WithDefault("false")
    boolean monitoringEnabled();

    /**
     * If set to true then the dependency graphs are generated and available in the Dev UI.
     */
    @WithDefault("true")
    boolean generateDependencyGraphs();

}

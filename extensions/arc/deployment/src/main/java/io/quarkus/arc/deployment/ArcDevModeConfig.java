package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ArcDevModeConfig {

    /**
     * If set to true then the container monitors business method invocations and fired events during the development mode.
     * <p>
     * NOTE: This config property should not be changed in the development mode as it requires a full rebuild of the application
     */
    @ConfigItem(defaultValue = "false")
    public boolean monitoringEnabled;

    /**
     * If set to true then the dependency graphs are generated and available in the Dev UI.
     */
    @ConfigItem(defaultValue = "true")
    public boolean generateDependencyGraphs;

}

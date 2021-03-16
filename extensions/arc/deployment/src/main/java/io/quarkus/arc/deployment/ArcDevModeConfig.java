package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ArcDevModeConfig {

    /**
     * If set to true then the container monitors business method invocations and fired events during the development mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean monitoringEnabled;

}

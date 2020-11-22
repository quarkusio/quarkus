package io.quarkus.unleash.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "unleash", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class UnleashRuntimeTimeConfig {

    /**
     * Unleash API service endpoint
     */
    @ConfigItem
    public String api;

    /**
     * Application name
     */
    @ConfigItem
    public String appName;

    /**
     * Instance ID.
     */
    @ConfigItem
    public Optional<String> instanceId = Optional.empty();

    /**
     * Disable Unleash metrics flag
     */
    @ConfigItem
    public boolean disableMetrics = false;
}

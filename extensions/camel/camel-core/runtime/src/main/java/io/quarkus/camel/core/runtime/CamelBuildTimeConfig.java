package io.quarkus.camel.core.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelBuildTimeConfig {

    /**
     * The class of the CamelRuntime implementation
     */
    @ConfigItem
    public Optional<String> runtime;

    /**
     * Uri to an xml containing camel routes to be loaded and initialized at build time.
     */
    @ConfigItem
    public Optional<String> routesUri;

}

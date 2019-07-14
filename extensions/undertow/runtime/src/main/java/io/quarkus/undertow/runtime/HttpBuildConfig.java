package io.quarkus.undertow.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration which applies to the HTTP server at build time.
 */
@ConfigRoot(name = "http", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HttpBuildConfig {

    /**
     * Enable the CORS filter.
     */
    @ConfigItem(name = "cors")
    public boolean corsEnabled = false;
}

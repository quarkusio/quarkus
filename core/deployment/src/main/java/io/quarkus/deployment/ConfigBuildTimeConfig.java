package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration
 */
@ConfigMapping(prefix = "quarkus.config")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigDocPrefix("quarkus.config")
public interface ConfigBuildTimeConfig {
    /**
     * <p>
     * Set this to <code>true</code> to read configuration from system properties and environment variables only. This
     * only applies to runtime.
     * </p>
     */
    @WithName("sources.system-only")
    @WithDefault("false")
    boolean systemOnly();
}

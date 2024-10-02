package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration
 */
@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.BUILD_TIME)
@ConfigDocPrefix("quarkus.config")
public class ConfigBuildTimeConfig {
    /**
     * <p>
     * Set this to <code>true</code> to read configuration from system properties and environment variables only. This
     * only applies to runtime.
     * </p>
     */
    @ConfigItem(name = "config.sources.system-only", defaultValue = "false")
    public boolean systemOnly;
}

package io.quarkus.vault.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HealthConfig {

    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Specifies if being a standby should still return the active status code instead of the standby status code.
     */
    @ConfigItem(defaultValue = "false")
    public boolean standByOk;

    /**
     * Specifies if being a performance standby should still return the active status code instead of the performance standby
     * status code.
     */
    @ConfigItem(defaultValue = "false")
    public boolean performanceStandByOk;

}

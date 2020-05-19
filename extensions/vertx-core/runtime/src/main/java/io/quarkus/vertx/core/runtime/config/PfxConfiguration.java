package io.quarkus.vertx.core.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PfxConfiguration {

    /**
     * PFX config is disabled by default.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled;

    /**
     * Path to the key file (PFX format).
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Password of the key.
     */
    @ConfigItem
    public Optional<String> password;
}

package io.quarkus.vertx.core.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JksConfiguration {

    /**
     * JKS config is disabled by default.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled;

    /**
     * Path of the key file (JKS format).
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Password of the key file.
     */
    @ConfigItem
    public Optional<String> password;
}

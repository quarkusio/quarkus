package io.quarkus.vertx.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PfxConfiguration {

    /**
     * Path to the key file (PFX format)
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Password of the key.
     */
    @ConfigItem
    public Optional<String> password;
}

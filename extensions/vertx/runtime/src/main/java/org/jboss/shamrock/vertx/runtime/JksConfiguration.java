package io.quarkus.vertx.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class JksConfiguration {

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

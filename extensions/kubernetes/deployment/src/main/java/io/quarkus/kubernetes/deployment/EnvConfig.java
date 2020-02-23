package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class EnvConfig {

    /**
     * The environment variable name.
     */
    @ConfigItem
    Optional<String> name;

    /**
     * The environment variable value.
     */
    @ConfigItem
    Optional<String> value;

    /**
     * The environment variable secret.
     */
    @ConfigItem
    Optional<String> secret;

    /**
     * The environment variable config map.
     */
    @ConfigItem
    Optional<String> configmap;

    /**
     * The environment variable field.
     */
    @ConfigItem
    Optional<String> field;

}

package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The configuration specifying which environment variables to inject into the application's container.
 */
@ConfigGroup
public class EnvVarsConfig {
    /**
     * The optional list of Secret names to load environment variables from.
     */
    @ConfigItem
    Optional<List<String>> secrets;

    /**
     * The optional list of ConfigMap names to load environment variables from.
     */
    @ConfigItem
    Optional<List<String>> configmaps;

    /**
     * The map associating environment variable names to their associated field references they take their value from.
     */
    @ConfigItem
    Map<String, String> fields;

    /**
     * The map associating environment name to its associated value.
     */
    @ConfigItem
    Map<String, String> vars;

    /**
     * The map recording the configuration of environment variable taking their value from resource (Secret or
     * ConfigMap) keys
     */
    @ConfigItem
    Map<String, EnvVarFromKeyConfig> mapping;
}

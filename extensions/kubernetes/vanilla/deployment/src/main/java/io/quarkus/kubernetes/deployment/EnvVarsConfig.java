package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * The configuration specifying which environment variables to inject into the application's container.
 */
public interface EnvVarsConfig {
    /**
     * The optional list of Secret names to load environment variables from.
     */
    Optional<List<String>> secrets();

    /**
     * The optional list of ConfigMap names to load environment variables from.
     */
    Optional<List<String>> configmaps();

    /**
     * The map associating environment variable names to their associated field references they take their value from.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> fields();

    /**
     * The map associating environment name to its associated value.
     */
    Map<String, Value> vars();

    /**
     * The map recording the configuration of environment variable taking their value from resource (Secret or
     * ConfigMap) keys
     */
    Map<String, EnvVarFromKeyConfig> mapping();

    /**
     * The map recording the configuration of environment variable prefix.
     */
    @WithName("using-prefix")
    Map<String, EnvVarPrefixConfig> prefixes();

    interface Value {
        /**
         * The environment variable value
         */
        @WithParentName
        Optional<String> value();
    }
}

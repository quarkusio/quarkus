package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface EnvConfig {
    /**
     * The environment variable name.
     */
    Optional<String> name();

    /**
     * The environment variable value.
     */
    Optional<String> value();

    /**
     * The environment variable secret.
     */
    Optional<String> secret();

    /**
     * The environment variable config map.
     */
    Optional<String> configmap();

    /**
     * The environment variable field.
     */
    Optional<String> field();
}

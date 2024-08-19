package io.quarkus.devservices.deployment.any;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface EnvValueConfig {

    /**
     * The value for the env var
     */
    Optional<String> value();

}

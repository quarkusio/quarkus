package io.quarkus.devservices.deployment.any;

import java.util.Optional;

import org.testcontainers.containers.BindMode;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface FileSystemBindConfig {

    /**
     * The value for the container path
     */
    Optional<String> containerPath();

    /**
     * The Bind mode of the volume
     */
    @WithDefault("READ_ONLY")
    BindMode bindMode();

}

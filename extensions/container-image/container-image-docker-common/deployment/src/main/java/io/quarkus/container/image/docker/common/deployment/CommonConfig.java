package io.quarkus.container.image.docker.common.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface CommonConfig {
    /**
     * Path to the JVM Dockerfile.
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root.
     * If not set src/main/docker/Dockerfile.jvm will be used.
     */
    @ConfigDocDefault("src/main/docker/Dockerfile.jvm")
    Optional<String> dockerfileJvmPath();

    /**
     * Path to the native Dockerfile.
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root.
     * If not set src/main/docker/Dockerfile.native will be used.
     */
    @ConfigDocDefault("src/main/docker/Dockerfile.native")
    Optional<String> dockerfileNativePath();

    /**
     * Build args passed to docker via {@code --build-arg}
     */
    @ConfigDocMapKey("arg-name")
    Map<String, String> buildArgs();

    /**
     * Images to consider as cache sources. Values are passed to {@code docker build}/{@code podman build} via the
     * {@code cache-from} option
     */
    Optional<List<String>> cacheFrom();

    /**
     * The networking mode for the RUN instructions during build
     */
    Optional<String> network();

    /**
     * Name of binary used to execute the docker/podman commands.
     * This setting can override the global container runtime detection.
     */
    Optional<String> executableName();

    /**
     * Additional arbitrary arguments passed to the executable when building the container image.
     */
    Optional<List<String>> additionalArgs();
}

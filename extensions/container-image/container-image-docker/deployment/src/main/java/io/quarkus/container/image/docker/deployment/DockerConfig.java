package io.quarkus.container.image.docker.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class DockerConfig {

    /**
     * Path to the the JVM Dockerfile.
     * If not set ${project.root}/src/main/docker/Dockerfile.jvm will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    public Optional<String> dockerfileJvmPath;

    /**
     * Path to the the JVM Dockerfile.
     * If not set ${project.root}/src/main/docker/Dockerfile.native will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    public Optional<String> dockerfileNativePath;
}

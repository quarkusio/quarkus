package io.quarkus.container.image.docker.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
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
    @ConfigItem
    public Optional<String> dockerfileJvmPath;

    /**
     * Path to the the JVM Dockerfile.
     * If not set ${project.root}/src/main/docker/Dockerfile.native will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    @ConfigItem
    public Optional<String> dockerfileNativePath;

    /**
     * Build args passed to docker via {@code --build-arg}
     */
    @ConfigItem
    public Map<String, String> buildArgs;

    /**
     * Images to consider as cache sources. Values are passed to {@code docker build} via the {@code cache-from} option
     */
    @ConfigItem
    public Optional<List<String>> cacheFrom;

    /**
     * Name of binary used to execute the docker commands.
     */
    @ConfigItem(defaultValue = "docker")
    public String executableName;
}

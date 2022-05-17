package io.quarkus.container.image.docker.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class DockerConfig {

    /**
     * Path to the JVM Dockerfile.
     * If not set ${project.root}/src/main/docker/Dockerfile.jvm will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    @ConfigItem
    public Optional<String> dockerfileJvmPath;

    /**
     * Path to the JVM Dockerfile.
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
     * The networking mode for the RUN instructions during build
     */
    public Optional<String> network;

    /**
     * Name of binary used to execute the docker commands.
     */
    @ConfigItem(defaultValue = "docker")
    public String executableName;

    /**
     * Configuration for Docker Buildx options
     */
    @ConfigItem
    @ConfigDocSection
    public DockerBuildxConfig buildx;

    /**
     * Configuration for Docker Buildx options. These are only relevant if using Docker Buildx
     * (https://docs.docker.com/buildx/working-with-buildx/#build-multi-platform-images) to build multi-platform (or
     * cross-platform)
     * images.
     * If any of these configurations are set, it will add {@code buildx} to the {@code executableName}.
     */
    @ConfigGroup
    public static class DockerBuildxConfig {
        /**
         * Which platform(s) to target during the build. See
         * https://docs.docker.com/engine/reference/commandline/buildx_build/#platform
         */
        @ConfigItem
        public Optional<List<String>> platform;

        /**
         * Sets the export action for the build result. See
         * https://docs.docker.com/engine/reference/commandline/buildx_build/#output. Note that any filesystem paths need to be
         * absolute paths,
         * not relative from where the command is executed from.
         */
        @ConfigItem
        public Optional<String> output;

        /**
         * Set type of progress output ({@code auto}, {@code plain}, {@code tty}). Use {@code plain} to show container output
         * (default “{@code auto}”). See https://docs.docker.com/engine/reference/commandline/buildx_build/#progress
         */
        @ConfigItem
        public Optional<String> progress;

        boolean useBuildx() {
            return platform.filter(p -> !p.isEmpty()).isPresent() ||
                    output.isPresent() ||
                    progress.isPresent();
        }
    }
}
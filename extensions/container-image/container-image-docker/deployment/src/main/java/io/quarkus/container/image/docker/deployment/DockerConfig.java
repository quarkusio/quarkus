package io.quarkus.container.image.docker.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.container.image.docker.common.deployment.CommonConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.docker")
public interface DockerConfig extends CommonConfig {
    /**
     * Configuration for Docker Buildx options
     */
    @ConfigDocSection
    DockerBuildxConfig buildx();

    /**
     * Configuration for Docker Buildx options. These are only relevant if using Docker Buildx
     * (https://docs.docker.com/buildx/working-with-buildx/#build-multi-platform-images) to build multi-platform (or
     * cross-platform)
     * images.
     * If any of these configurations are set, it will add {@code buildx} to the {@code executableName}.
     */
    @ConfigGroup
    interface DockerBuildxConfig {
        /**
         * Which platform(s) to target during the build. See
         * https://docs.docker.com/engine/reference/commandline/buildx_build/#platform
         */
        Optional<List<String>> platform();

        /**
         * Sets the export action for the build result. See
         * https://docs.docker.com/engine/reference/commandline/buildx_build/#output. Note that any filesystem paths need to be
         * absolute paths,
         * not relative from where the command is executed from.
         */
        Optional<String> output();

        /**
         * Set type of progress output ({@code auto}, {@code plain}, {@code tty}). Use {@code plain} to show container output
         * (default “{@code auto}”). See https://docs.docker.com/engine/reference/commandline/buildx_build/#progress
         */
        Optional<String> progress();

        default boolean useBuildx() {
            return platform().filter(p -> !p.isEmpty()).isPresent() ||
                    output().isPresent() ||
                    progress().isPresent();
        }
    }
}

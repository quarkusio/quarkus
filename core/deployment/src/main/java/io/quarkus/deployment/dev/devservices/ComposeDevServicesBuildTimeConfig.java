package io.quarkus.deployment.dev.devservices;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ComposeDevServicesBuildTimeConfig {

    /**
     * Compose dev service enabled or disabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * List of file paths relative to the project root for Compose dev service configuration,
     * if not provided will look for compose files in the project root
     */
    Optional<List<String>> files();

    /**
     * Name of the compose project, used to discover running containers,
     * if not provided a project name will be generated
     */
    Optional<String> projectName();

    /**
     * Compose profiles to activate
     */
    Optional<List<String>> profiles();

    /**
     * List of additional options to pass to compose command
     */
    Optional<List<String>> options();

    /**
     * Whether to run compose up and start containers at startup, when disabled, services are discovered by project name
     */
    @WithDefault("true")
    boolean startServices();

    /**
     * Whether to run compose down and stop containers at shutdown
     */
    @WithDefault("true")
    boolean stopServices();

    /**
     * Whether to use test containers Ryuk resource reaper to clean up containers
     */
    @WithDefault("true")
    boolean ryukEnabled();

    /**
     * Whether to remove volumes on compose down
     */
    @WithDefault("true")
    boolean removeVolumes();

    /**
     * Which images to remove on compose down
     * <p>
     * Locally built images, without custom tags are removed by default.
     */
    Optional<RemoveImages> removeImages();

    /**
     * --rmi option for compose down
     */
    enum RemoveImages {
        ALL,
        LOCAL
    }

    /**
     * Env variables to pass to all Compose instances
     */
    Map<String, String> envVariables();

    /**
     * Scale configuration for services: Configure the number of instances for specific services
     */
    Map<String, Integer> scale();

    /**
     * Whether to tail container logs to the console
     */
    @WithDefault("false")
    boolean followContainerLogs();

    /**
     * Whether to build images before starting containers.
     * <p>
     * When not provided, Compose images are built per-service `pull-policy`.
     * When `true`, forces build of all images before starting containers.
     * When `false`, skips re-building images before starting containers.
     */
    Optional<Boolean> build();

    /**
     * Whether to reuse the project for tests, when disabled, a new project is created for each test run
     */
    @WithDefault("false")
    boolean reuseProjectForTests();

    /**
     * Timeout for stopping services, after the timeout the services are forcefully stopped,
     *
     */
    @WithDefault("1s")
    Duration stopTimeout();
}

package io.quarkus.container.image.buildpack.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class BuildpackConfig {

    /**
     * The buildpacks builder image to use when building the project.
     */
    @ConfigItem(defaultValue = "paketobuildpacks/builder:full")
    public String builderImage;

    /**
     * The buildpacks run image to use when building the project
     * 
     * When not supplied, the run image is determined by the builder image.
     */
    @ConfigItem
    public Optional<String> runImage;

    /**
     * Max pull timeout for builder/run images, in seconds
     */
    @ConfigItem
    public Optional<Integer> pullTimeoutSeconds;

    /**
     * DOCKER_HOST value to use.
     * 
     * If not set, the env var DOCKER_HOST is used, if that is not set
     * the value `unix:///var/run/docker.sock' (or 'npipe:///./pipe/docker_engine' for windows) is used.
     */
    @ConfigItem
    public Optional<String> dockerHost;

    /**
     * Log level to use..
     * Defaults to 'info'
     */
    @ConfigItem
    public Optional<String> logLevel;

    /**
     * The username to use to authenticate with the registry used to pull the base JVM image
     */
    @ConfigItem
    public Optional<String> baseRegistryUsername;

    /**
     * The password to use to authenticate with the registry used to pull the base JVM image
     */
    @ConfigItem
    public Optional<String> baseRegistryPassword;

}

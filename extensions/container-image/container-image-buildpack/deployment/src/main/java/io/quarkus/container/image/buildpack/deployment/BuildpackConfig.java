package io.quarkus.container.image.buildpack.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class BuildpackConfig {

    /**
     * The buildpacks builder image to use when building the project in jvm mode.
     */
    @ConfigItem(defaultValue = "paketocommunity/builder-ubi-base:latest")
    public String jvmBuilderImage;

    /**
     * The buildpacks builder image to use when building the project in native mode.
     */
    @ConfigItem
    public Optional<String> nativeBuilderImage;

    /**
     * Should the builder image be 'trusted' (use creator lifecycle)
     */
    @ConfigItem
    public Optional<Boolean> trustBuilderImage;

    /**
     * Environment key/values to pass to buildpacks.
     */
    @ConfigItem
    @ConfigDocMapKey("environment-variable-name")
    public Map<String, String> builderEnv;

    /**
     * The buildpacks run image to use when building the project
     *
     * When not supplied, the run image is determined by the builder image.
     */
    @ConfigItem
    public Optional<String> runImage;

    /**
     * Initial pull timeout for builder/run images, in seconds
     */
    @ConfigItem(defaultValue = "300")
    public Integer pullTimeoutSeconds;

    /**
     * Increase pull timeout for builder/run images after failure, in seconds
     */
    @ConfigItem(defaultValue = "15")
    public Integer pullTimeoutIncreaseSeconds;

    /**
     * How many times to retry an image pull after a failure
     */
    @ConfigItem(defaultValue = "3")
    public Integer pullRetryCount;

    /**
     * DOCKER_HOST value to use.
     *
     * If not set, the env var DOCKER_HOST is used, if that is not set the platform will look for
     * 'npipe:///./pipe/docker_engine' for windows, and `unix:///var/run/docker.sock' then
     * `unix:///var/run/podman.sock` then `unix:///var/run/user/<uid>/podman/podman.sock` for linux
     */
    @ConfigItem
    public Optional<String> dockerHost;

    /**
     * Log level to use..
     * Defaults to 'info'
     */
    @ConfigItem(defaultValue = "info")
    public String logLevel;

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

package io.quarkus.container.image.buildpack.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.buildpack")
public interface BuildpackConfig {

    /**
     * The buildpacks builder image to use when building the project in jvm mode.
     */
    @WithDefault("paketocommunity/builder-ubi-base:latest")
    String jvmBuilderImage();

    /**
     * The buildpacks builder image to use when building the project in native mode.
     */
    Optional<String> nativeBuilderImage();

    /**
     * Should the builder image be 'trusted' (use creator lifecycle)
     */
    Optional<Boolean> trustBuilderImage();

    /**
     * Environment key/values to pass to buildpacks.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> builderEnv();

    /**
     * The buildpacks run image to use when building the project
     *
     * When not supplied, the run image is determined by the builder image.
     */
    Optional<String> runImage();

    /**
     * Initial pull timeout for builder/run images, in seconds
     */
    @WithDefault("300")
    Integer pullTimeoutSeconds();

    /**
     * Increase pull timeout for builder/run images after failure, in seconds
     */
    @WithDefault("15")
    Integer pullTimeoutIncreaseSeconds();

    /**
     * How many times to retry an image pull after a failure
     */
    @WithDefault("3")
    Integer pullRetryCount();

    /**
     * DOCKER_HOST value to use.
     *
     * If not set, the env var DOCKER_HOST is used, if that is not set the platform will look for
     * 'npipe:///./pipe/docker_engine' for windows, and `unix:///var/run/docker.sock' then
     * `unix:///var/run/podman.sock` then `unix:///var/run/user/<uid>/podman/podman.sock` for linux
     */
    Optional<String> dockerHost();

    /**
     * use Daemon mode?
     * Defaults to 'true'
     */
    @WithDefault("true")
    Boolean useDaemon();

    /**
     * Use specified docker network during build
     */
    Optional<String> dockerNetwork();

    /**
     * Log level to use.
     * Defaults to 'info'
     */
    @WithDefault("info")
    String logLevel();

    /**
     * The username to use to authenticate with the registry used to pull the base JVM image
     */
    Optional<String> baseRegistryUsername();

    /**
     * The password to use to authenticate with the registry used to pull the base JVM image
     */
    Optional<String> baseRegistryPassword();

}

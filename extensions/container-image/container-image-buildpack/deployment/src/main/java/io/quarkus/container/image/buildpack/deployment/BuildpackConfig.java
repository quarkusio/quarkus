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
     * The lifecycle image to use when building the project
     *
     * This is optional, but can be used to override the lifecycle present within a builder image.
     */
    Optional<String> lifecycleImage();

    /**
     * The platform level to force for the build.
     *
     * Normally the platform level is determined from the intersection of the builder image supported
     * levels, and the platform implementation supported levels. Sometimes it can be beneficial to force
     * the platform to a particular version to force behavior during the build.
     */
    Optional<String> platformLevel();

    /**
     * Should the builder image be 'trusted' ?
     *
     * Trusted builders are allowed to attempt to use the `creator` lifecycle, which runs all the
     * build phases within a single container. This is only possible for builders that do not use
     * extensions. Running all phases in one container exposes some phases to information they may
     * not see normally with a container-per-phase.
     */
    Optional<Boolean> trustBuilderImage();

    /**
     * Environment key/values to pass to buildpacks.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> builderEnv();

    /**
     * Usernames to use with registry hosts
     */
    @ConfigDocMapKey("registry-host")
    Map<String, String> registryUser();

    /**
     * Passwords to use with registry hosts
     */
    @ConfigDocMapKey("registry-host")
    Map<String, String> registryPassword();

    /**
     * Tokens to use with registry hosts
     */
    @ConfigDocMapKey("registry-host")
    Map<String, String> registryToken();

    /**
     * The buildpacks run image to use when building the project
     *
     * When not supplied, the run image is determined by the builder image.
     * If extensions are used by the builder image, they may override the run image.
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
     * This value is normally auto-determined, and is available for override if needed.
     *
     * If not set, the env var DOCKER_HOST is used, if that is not set the platform will
     * test if `podman` is available on the path, if so, it will use podman to configure the
     * appropriate values. If `podman` is not on the path, docker is assumed, and per-platform
     * defaults for docker are used.
     */
    Optional<String> dockerHost();

    /**
     * Path to the Docker socket to use.
     *
     * This value is normally auto-determined, and is available for override if needed.
     *
     * The path to the socket can vary, especially when the docker/podman daemon is running inside
     * a VM, if useDaemon mode is true, then this path must refer to the path that can be used to
     * mount the socket inside a container, so may refer to the path to the socket in the VM rather
     * than the host.
     */
    Optional<String> dockerSocket();

    /**
     * use Daemon mode?
     *
     * Should the buildpack build have the docker socket mounted into the build container(s).
     * If this is false, then the image will be built directly as layers in a remote registry,
     * this will probably require registry credentials to be passed.
     *
     * Defaults to 'true'
     */
    @WithDefault("true")
    Boolean useDaemon();

    /**
     * Use specified docker network during build
     *
     * This can be handy when building against a locally hosted docker registry, where you
     * will require the build containers to be part of the 'host' network to enable them
     * to access the local registry.
     */
    Optional<String> dockerNetwork();

    /**
     * Log level to use.
     *
     * The log level to use when executing the build phases in containers.
     *
     * Defaults to 'info', supported values are 'info', 'warn' and 'debug'
     */
    @WithDefault("info")
    String logLevel();

    /**
     * Should the container log information include timestamps?
     */
    @WithDefault("true")
    Boolean getUseTimestamps();

}

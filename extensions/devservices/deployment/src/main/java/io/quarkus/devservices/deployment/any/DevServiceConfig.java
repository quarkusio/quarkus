package io.quarkus.devservices.deployment.any;

import java.net.URL;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DevServiceConfig {

    /**
     * Whether this dev service should be enabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The image to use
     */
    Optional<String> imageName();

    /**
     * The container port. If not provided no port will be mapped
     */
    OptionalInt containerPort();

    /**
     * The port that the contain port should be mapped to. If not provided a random port will be used
     */
    OptionalInt mappedPort();

    /**
     * The version to use
     */
    @WithDefault("latest")
    String version();

    /**
     * The url the service started on. Might be nothing
     */
    Optional<URL> url();

    /**
     * If the network should be SHARED
     */
    @WithDefault("true")
    boolean sharedNetwork();

    /**
     * If the container should be reused
     */
    Optional<Boolean> reuse();

    /**
     * Access to the host
     */
    @WithDefault("false")
    boolean accessToHost();

    /**
     * Capture log
     */
    @WithDefault("false")
    boolean captureLog();

    /**
     * Wait for
     */
    @ConfigDocSection
    WaitForConfig waitFor();

    /**
     * Labels
     */
    @ConfigDocSection
    LabelsConfig label();

    /**
     * Env vars
     */
    @ConfigDocSection
    EnvVarsConfig env();

    /**
     * File System bindings
     */
    @ConfigDocSection
    FileSystemBindsConfig fileSystemBindings();

    /**
     * Classpath Resource mappings
     */
    @ConfigDocSection
    ClasspathResourceMappingsConfig classpathResourceMappings();
}

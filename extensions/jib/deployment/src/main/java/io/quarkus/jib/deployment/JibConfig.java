package io.quarkus.jib.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class JibConfig {

    /**
     * If enabled, a container image will be created using jib
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The group the container image will be part of
     */
    @ConfigItem(defaultValue = "${user.name}")
    public String group;

    /**
     * The name of the container image. If not set defaults to the application name
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * The tag of the container image. If not set defaults to the application version
     */
    @ConfigItem
    public Optional<String> tag;

    /**
     * The base image to be used when a container image is being produced for the jar build
     */
    @ConfigItem(defaultValue = "fabric8/java-alpine-openjdk8-jre")
    public String baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build
     */
    @ConfigItem(defaultValue = "registry.access.redhat.com/ubi8/ubi-minimal")
    public String baseNativeImage;

    /**
     * Additional JVM arguments to pass to the JVM when starting the application
     */
    @ConfigItem(defaultValue = "-Dquarkus.http.host=0.0.0.0,-Djava.util.logging.manager=org.jboss.logmanager.LogManager")
    public List<String> jvmArguments;

    /**
     * Additional arguments to pass when starting the native application
     */
    @ConfigItem(defaultValue = "-Dquarkus.http.host=0.0.0.0")
    public List<String> nativeArguments;

    /**
     * Environment variables to add to the container image
     */
    @ConfigItem
    public Optional<Map<String, String>> environmentVariables;

    /**
     * If enabled, the image will be pushed to the registry instead of being built locally
     */
    @ConfigItem(defaultValue = "false")
    public boolean push;

    /**
     * The container registry to use
     */
    @ConfigItem(defaultValue = "docker.io")
    public String registry;

    /**
     * The username to use to authenticate with the registry
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password to use to authenticate with the registry
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Whether or not insecure registries are allowed
     */
    @ConfigItem(defaultValue = "false")
    public boolean insecure;
}

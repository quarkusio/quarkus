package io.quarkus.container.image.jib.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class JibConfig {

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

    //TODO: do the following config options belong in ContainerImageConfig ?

}

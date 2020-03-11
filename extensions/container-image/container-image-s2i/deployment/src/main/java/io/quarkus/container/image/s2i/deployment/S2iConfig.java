package io.quarkus.container.image.s2i.deployment;

import java.time.Duration;
import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class S2iConfig {

    public static final String DEFAULT_BASE_JVM_IMAGE = "fabric8/s2i-java:2.3";
    public static final String DEFAULT_BASE_NATIVE_IMAGE = "quay.io/quarkus/ubi-quarkus-native-binary-s2i:19.3.0";

    /**
     * The base image to be used when a container image is being produced for the jar build
     */
    @ConfigItem(defaultValue = DEFAULT_BASE_JVM_IMAGE)
    public String baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build
     */
    @ConfigItem(defaultValue = DEFAULT_BASE_NATIVE_IMAGE)
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
     * The path to where the jar is added during the assemble phase.
     * This is dependant on the s2i image and should be supplied if a non default image is used.
     */
    @ConfigItem(defaultValue = "/deployments/application${quarkus.package.runner-suffix}.jar")
    public String jarPath;

    /**
     * The path to where the native binary is added during the assemble phase.
     * This is dependant on the s2i image and should be supplied if a non default image is used.
     */
    @ConfigItem(defaultValue = "/home/quarkus/application")
    public String nativeBinaryPath;

    /**
     * The build timeout.
     */
    @ConfigItem(defaultValue = "PT5M")
    Duration buildTimeout;

    /**
     * Check if baseJvmImage is the default
     * 
     * @returns true if baseJvmImage is the default
     */
    public boolean hasDefaultBaseJvmImage() {
        return baseJvmImage.equals(DEFAULT_BASE_JVM_IMAGE);
    }

}

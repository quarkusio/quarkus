package io.quarkus.container.image.openshift.deployment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.images.ContainerImages;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "openshift", phase = ConfigPhase.BUILD_TIME)
public class ContainerImageOpenshiftConfig {

    public static final String DEFAULT_NATIVE_TARGET_FILENAME = "application";

    public static final String DEFAULT_JVM_DOCKERFILE = "src/main/docker/Dockerfile.jvm";
    public static final String DEFAULT_NATIVE_DOCKERFILE = "src/main/docker/Dockerfile.native";

    public static final String DEFAULT_BUILD_LOG_LEVEL = "INFO";

    public static final String FALLBACK_JAR_DIRECTORY = "/deployments/";
    public static final String FALLBACK_NATIVE_BINARY_DIRECTORY = "/home/quarkus/";

    public static String getDefaultJvmImage(CompiledJavaVersionBuildItem.JavaVersion version) {
        if (version.isJava21OrHigher() == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return ContainerImages.S2I_JAVA_21;
        }
        return ContainerImages.S2I_JAVA_17;
    }

    /**
     * The build config strategy to use.
     */
    @ConfigItem(defaultValue = "binary")
    public BuildStrategy buildStrategy;

    /**
     * The base image to be used when a container image is being produced for the jar build.
     * The value of this property is used to create an ImageStream for the builder image used in the Openshift build.
     * When it references images already available in the internal Openshift registry, the corresponding streams are used
     * instead.
     * When the application is built against Java 21 or higher, {@code registry.access.redhat.com/ubi8/openjdk-21:1.20}
     * is used as the default.
     * Otherwise {@code registry.access.redhat.com/ubi8/openjdk-17:1.20} is used as the default.
     */
    @ConfigItem
    public Optional<String> baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build.
     * The value of this property is used to create an ImageStream for the builder image used in the Openshift build.
     * When it references images already available in the internal Openshift registry, the corresponding streams are used
     * instead.
     */
    @ConfigItem(defaultValue = ContainerImages.QUARKUS_BINARY_S2I)
    public String baseNativeImage;

    /**
     * The default Dockerfile to use for jvm builds
     */
    @ConfigItem(defaultValue = DEFAULT_JVM_DOCKERFILE)
    public String jvmDockerfile;

    /**
     * The default Dockerfile to use for native builds
     */
    @ConfigItem(defaultValue = DEFAULT_NATIVE_DOCKERFILE)
    public String nativeDockerfile;

    /**
     * The JVM arguments to pass to the JVM when starting the application
     */
    @ConfigItem
    public Optional<List<String>> jvmArguments;

    /**
     * Additional arguments to pass when starting the native application
     */
    @ConfigItem
    public Optional<List<String>> nativeArguments;

    /**
     * The directory where the jar is added during the assemble phase.
     * This is dependent on the S2I image and should be supplied if a non default image is used.
     */
    @ConfigItem
    public Optional<String> jarDirectory;

    /**
     * The resulting filename of the jar in the S2I image.
     * This option may be used if the selected S2I image uses a fixed name for the jar.
     */
    @ConfigItem
    public Optional<String> jarFileName;

    /**
     * The directory where the native binary is added during the assemble phase.
     * This is dependent on the S2I image and should be supplied if a non-default image is used.
     */
    @ConfigItem
    public Optional<String> nativeBinaryDirectory;

    /**
     * The resulting filename of the native binary in the S2I image.
     * This option may be used if the selected S2I image uses a fixed name for the native binary.
     */
    @ConfigItem
    public Optional<String> nativeBinaryFileName;

    /**
     * The build timeout.
     */
    @ConfigItem(defaultValue = "PT5M")
    Duration buildTimeout;

    /**
     * The log level of OpenShift build log.
     */
    @ConfigItem(defaultValue = DEFAULT_BUILD_LOG_LEVEL)
    public Logger.Level buildLogLevel;

    /**
     * The image push secret to use for pushing to external registries.
     * (see: https://cloud.redhat.com/blog/pushing-application-images-to-an-external-registry)
     **/
    @ConfigItem
    public Optional<String> imagePushSecret;

    /**
     * Check if baseJvmImage is the default
     *
     * @returns true if baseJvmImage is the default
     */
    public boolean hasDefaultBaseJvmImage() {
        return baseJvmImage.isPresent();
    }

    /**
     * Check if baseNativeImage is the default
     *
     * @returns true if baseNativeImage is the default
     */
    public boolean hasDefaultBaseNativeImage() {
        return baseNativeImage.equals(ContainerImages.QUARKUS_BINARY_S2I);
    }

    /**
     * Check if jvmDockerfile is the default
     *
     * @returns true if jvmDockerfile is the default
     */
    public boolean hasDefaultJvmDockerfile() {
        return jvmDockerfile.equals(DEFAULT_JVM_DOCKERFILE);
    }

    /**
     * Check if nativeDockerfile is the default
     *
     * @returns true if nativeDockerfile is the default
     */
    public boolean hasDefaultNativeDockerfile() {
        return nativeDockerfile.equals(DEFAULT_NATIVE_DOCKERFILE);
    }

    /**
     * @return the effective JVM arguments to use by getting the jvmArguments and the jvmAdditionalArguments properties.
     */
    public List<String> getEffectiveJvmArguments() {
        List<String> effectiveJvmArguments = new ArrayList<>();
        jvmArguments.ifPresent(effectiveJvmArguments::addAll);
        return effectiveJvmArguments;
    }

}

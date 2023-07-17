package io.quarkus.container.image.openshift.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class S2iConfig {

    public static final String DEFAULT_BASE_JVM_JDK11_IMAGE = "registry.access.redhat.com/ubi8/openjdk-11:1.16";
    public static final String DEFAULT_BASE_JVM_JDK17_IMAGE = "registry.access.redhat.com/ubi8/openjdk-17:1.16";
    public static final String DEFAULT_BASE_NATIVE_IMAGE = "quay.io/quarkus/ubi-quarkus-native-binary-s2i:2.0";
    public static final String DEFAULT_NATIVE_TARGET_FILENAME = "application";

    public static final String DEFAULT_JVM_DOCKERFILE = "src/main/docker/Dockerfile.jvm";
    public static final String DEFAULT_NATIVE_DOCKERFILE = "src/main/docker/Dockerfile.native";

    public static final String FALLBACK_JAR_DIRECTORY = "/deployments/";
    public static final String FALLBACK_NATIVE_BINARY_DIRECTORY = "/home/quarkus/";

    public static String getDefaultJvmImage(CompiledJavaVersionBuildItem.JavaVersion version) {
        switch (version.isJava17OrHigher()) {
            case TRUE:
                return DEFAULT_BASE_JVM_JDK17_IMAGE;
            default:
                return DEFAULT_BASE_JVM_JDK11_IMAGE;
        }
    }

    /**
     * The build config strategy to use.
     */
    @ConfigItem(defaultValue = "binary")
    public BuildStrategy buildStrategy;

    /**
     * The base image to be used when a container image is being produced for the jar build.
     *
     * When the application is built against Java 17 or higher, {@code registry.access.redhat.com/ubi8/openjdk-17:1.16}
     * is used as the default.
     * Otherwise {@code registry.access.redhat.com/ubi8/openjdk-11:1.16} is used as the default.
     */
    @ConfigItem
    public Optional<String> baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build
     */
    @ConfigItem(defaultValue = DEFAULT_BASE_NATIVE_IMAGE)
    public String baseNativeImage;

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
    @ConfigItem(defaultValue = "/deployments/target/")
    public String jarDirectory;

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
    @ConfigItem(defaultValue = "/home/quarkus/")
    public String nativeBinaryDirectory;

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
        return baseNativeImage.equals(DEFAULT_BASE_NATIVE_IMAGE);
    }
}

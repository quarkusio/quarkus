package io.quarkus.deployment.images;

import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;

/**
 * This class is used to define the container images that are used by Quarkus.
 * <p>
 * For each image, the image name and version are defined as constants:
 * <p>
 * - {@code x_IMAGE_NAME} - the name of the image without the version (e.g. {@code registry.access.redhat.com/ubi8/ubi-minimal})
 * - {@code x_VERSION} - the version of the image (e.g. {@code 8.10})
 * - {@code x} - the full image name (e.g. {@code registry.access.redhat.com/ubi8/ubi-minimal:8.10})
 */
public class ContainerImages {

    // Global versions

    /**
     * UBI 8 version
     */
    public static final String UBI8_VERSION = "8.10";

    /**
     * UBI 8 version
     */
    public static final String UBI9_VERSION = "9.4";

    /**
     * Version used for more UBI Java images.
     */
    public static final String UBI8_JAVA_VERSION = "1.20";

    /**
     * Version uses for the native builder image.
     */
    public static final String NATIVE_BUILDER_VERSION = "jdk-21";

    // === Runtime images for containers (native)

    // UBI 8 Minimal - https://catalog.redhat.com/software/containers/ubi8/ubi-minimal/5c359a62bed8bd75a2c3fba8
    public static final String UBI8_MINIMAL_IMAGE_NAME = "registry.access.redhat.com/ubi8/ubi-minimal";
    public static final String UBI8_MINIMAL_VERSION = UBI8_VERSION;
    public static final String UBI8_MINIMAL = UBI8_MINIMAL_IMAGE_NAME + ":" + UBI8_MINIMAL_VERSION;

    // UBI 9 Minimal - https://catalog.redhat.com/software/containers/ubi9-minimal/61832888c0d15aff4912fe0d
    public static final String UBI9_MINIMAL_IMAGE_NAME = "registry.access.redhat.com/ubi9/ubi-minimal";
    public static final String UBI9_MINIMAL_VERSION = UBI9_VERSION;
    public static final String UBI9_MINIMAL = UBI9_MINIMAL_IMAGE_NAME + ":" + UBI9_MINIMAL_VERSION;

    // Quarkus Micro image - https://quay.io/repository/quarkus/quarkus-micro-image?tab=tags
    public static final String QUARKUS_MICRO_IMAGE_NAME = "quay.io/quarkus/quarkus-micro-image";
    public static final String QUARKUS_MICRO_VERSION = "2.0";
    public static final String QUARKUS_MICRO_IMAGE = QUARKUS_MICRO_IMAGE_NAME + ":" + QUARKUS_MICRO_VERSION;

    // === Runtime images for containers (JVM)

    // UBI 8 OpenJDK 17 Runtime - https://catalog.redhat.com/software/containers/ubi8/openjdk-17-runtime/618bdc5f843af1624c4e4ba8
    public static final String UBI8_JAVA_17_IMAGE_NAME = "registry.access.redhat.com/ubi8/openjdk-17-runtime";
    public static final String UBI8_JAVA_17_VERSION = UBI8_JAVA_VERSION;
    public static final String UBI8_JAVA_17 = UBI8_JAVA_17_IMAGE_NAME + ":" + UBI8_JAVA_17_VERSION;

    // UBI 8 OpenJDK 21 Runtime - https://catalog.redhat.com/software/containers/ubi8/openjdk-21-runtime/653fd184292263c0a2f14d69
    public static final String UBI8_JAVA_21_IMAGE_NAME = "registry.access.redhat.com/ubi8/openjdk-21-runtime";
    public static final String UBI8_JAVA_21_VERSION = UBI8_JAVA_VERSION;
    public static final String UBI8_JAVA_21 = UBI8_JAVA_21_IMAGE_NAME + ":" + UBI8_JAVA_21_VERSION;

    // UBI 9 OpenJDK 17 Runtime - https://catalog.redhat.com/software/containers/ubi9/openjdk-17-runtime/61ee7d45384a3eb331996bee
    public static final String UBI9_JAVA_17_IMAGE_NAME = "registry.access.redhat.com/ubi9/openjdk-17-runtime";
    public static final String UBI9_JAVA_17_VERSION = UBI8_JAVA_VERSION;
    public static final String UBI9_JAVA_17 = UBI9_JAVA_17_IMAGE_NAME + ":" + UBI9_JAVA_17_VERSION;

    // UBI 9 OpenJDK 21 Runtime - https://catalog.redhat.com/software/containers/ubi9/openjdk-21-runtime/6501ce769a0d86945c422d5f
    public static final String UBI9_JAVA_21_IMAGE_NAME = "registry.access.redhat.com/ubi9/openjdk-21-runtime";
    public static final String UBI9_JAVA_21_VERSION = UBI8_JAVA_VERSION;
    public static final String UBI9_JAVA_21 = UBI9_JAVA_21_IMAGE_NAME + ":" + UBI9_JAVA_21_VERSION;

    // === Source To Image images

    // Quarkus Binary Source To Image - https://quay.io/repository/quarkus/ubi-quarkus-native-binary-s2i?tab=tags
    public static final String QUARKUS_BINARY_S2I_IMAGE_NAME = "quay.io/quarkus/ubi-quarkus-native-binary-s2i";
    public static final String QUARKUS_BINARY_S2I_VERSION = "2.0";
    public static final String QUARKUS_BINARY_S2I = QUARKUS_BINARY_S2I_IMAGE_NAME + ":" + QUARKUS_BINARY_S2I_VERSION;

    // Java 17 Source To Image - https://catalog.redhat.com/software/containers/ubi8/openjdk-17/618bdbf34ae3739687568813
    public static final String S2I_JAVA_17_IMAGE_NAME = "registry.access.redhat.com/ubi8/openjdk-17";
    public static final String S2I_JAVA_17_VERSION = UBI8_JAVA_VERSION;
    public static final String S2I_JAVA_17 = S2I_JAVA_17_IMAGE_NAME + ":" + S2I_JAVA_17_VERSION;

    // Java Source To Image - https://catalog.redhat.com/software/containers/ubi8/openjdk-21/653fb7e21b2ec10f7dfc10d0?q=openjdk%2021&architecture=amd64&image=66bcc007a3857fbc34f4dce1
    public static final String S2I_JAVA_21_IMAGE_NAME = "registry.access.redhat.com/ubi8/openjdk-21";
    public static final String S2I_JAVA_21_VERSION = UBI8_JAVA_VERSION;
    public static final String S2I_JAVA_21 = S2I_JAVA_21_IMAGE_NAME + ":" + S2I_JAVA_21_VERSION;

    // === Native Builder images

    // Mandrel Builder Image - https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags
    public static final String MANDREL_BUILDER_IMAGE_NAME = "quay.io/quarkus/ubi-quarkus-mandrel-builder-image";
    public static final String MANDREL_BUILDER_VERSION = NATIVE_BUILDER_VERSION;
    public static final String MANDREL_BUILDER = MANDREL_BUILDER_IMAGE_NAME + ":" + MANDREL_BUILDER_VERSION;

    // GraalVM CE Builder Image - https://quay.io/repository/quarkus/ubi-quarkus-graalvmce-builder-image?tab=tags
    public static final String GRAALVM_BUILDER_IMAGE_NAME = "quay.io/quarkus/ubi-quarkus-graalvmce-builder-image";
    public static final String GRAALVM_BUILDER_VERSION = NATIVE_BUILDER_VERSION;
    public static final String GRAALVM_BUILDER = GRAALVM_BUILDER_IMAGE_NAME + ":" + GRAALVM_BUILDER_VERSION;

    public static String getDefaultJvmImage(CompiledJavaVersionBuildItem.JavaVersion version) {
        if (version.isJava21OrHigher() == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return UBI8_JAVA_21;
        } else {
            return UBI8_JAVA_17;
        }
    }
}

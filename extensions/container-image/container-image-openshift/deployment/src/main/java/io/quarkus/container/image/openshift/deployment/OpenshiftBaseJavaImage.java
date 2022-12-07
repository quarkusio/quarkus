
package io.quarkus.container.image.openshift.deployment;

import java.util.Optional;

import io.quarkus.container.image.deployment.util.ImageUtil;

public enum OpenshiftBaseJavaImage {

    //We only compare `repositories` so registries and tags are stripped
    FABRIC8("fabric8/s2i-java:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR", "JAVA_CLASSPATH",
            "JAVA_OPTIONS"),
    OPENJDK_8_RHEL7("redhat-openjdk-18/openjdk18-openshift:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR",
            "JAVA_LIB_DIR", "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJDK_8_RHEL8("openjdk/openjdk-8-rhel8:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJDK_11_RHEL7("openjdk/openjdk-11-rhel7:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJDK_11_RHEL8("openjdk/openjdk-11-rhel8:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJ9_8_RHEL7("openj9/openj9-8-rhel7:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJ9_8_RHEL8("openj9/openj9-8-rhel8:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJ9_11_RHEL7("openj9/openj9-11-rhel7:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJ9_11_RHEL8("openj9/openj9-11-rhel8:latest", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS"),
    OPENJDK_11_UBI8("ubi8/openjdk-11", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS", true),
    OPENJDK_17_UBI8("ubi8/openjdk-17", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS", true),
    OPENJDK_11_UBI9("ubi9/openjdk-11", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS", true),
    OPENJDK_17_UBI9("ubi9/openjdk-17", "/deployments", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR",
            "JAVA_CLASSPATH", "JAVA_OPTIONS", true);

    private final String image;
    private final String jarDirectory;
    private final String javaMainClassEnvVar;
    private final String jarEnvVar;
    private final String jarLibEnvVar;
    private final String classpathEnvVar;
    private final String jvmOptionsEnvVar;
    private final boolean requiresCommand;

    public static Optional<OpenshiftBaseJavaImage> findMatching(String image) {
        for (OpenshiftBaseJavaImage candidate : OpenshiftBaseJavaImage.values()) {
            if (ImageUtil.getRepository(candidate.getImage()).equals(ImageUtil.getRepository(image))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    OpenshiftBaseJavaImage(String image, String jarDirectory, String javaMainClassEnvVar, String jarEnvVar,
            String jarLibEnvVar, String classpathEnvVar, String jvmOptionsEnvVar) {
        this(image, jarDirectory, javaMainClassEnvVar, jarEnvVar, jarLibEnvVar, classpathEnvVar, jvmOptionsEnvVar, false);
    }

    OpenshiftBaseJavaImage(String image, String jarDirectory, String javaMainClassEnvVar, String jarEnvVar,
            String jarLibEnvVar, String classpathEnvVar, String jvmOptionsEnvVar, boolean requiresCommand) {
        this.image = image;
        this.jarDirectory = jarDirectory;
        this.javaMainClassEnvVar = javaMainClassEnvVar;
        this.jarEnvVar = jarEnvVar;
        this.jarLibEnvVar = jarLibEnvVar;
        this.classpathEnvVar = classpathEnvVar;
        this.jvmOptionsEnvVar = jvmOptionsEnvVar;
        this.requiresCommand = requiresCommand;
    }

    public String getImage() {
        return image;
    }

    public String getJarDirectory() {
        return this.jarDirectory;
    }

    public String getJavaMainClassEnvVar() {
        return javaMainClassEnvVar;
    }

    public String getJvmOptionsEnvVar() {
        return jvmOptionsEnvVar;
    }

    public String getClasspathEnvVar() {
        return classpathEnvVar;
    }

    public String getJarLibEnvVar() {
        return jarLibEnvVar;
    }

    public String getJarEnvVar() {
        return jarEnvVar;
    }

    public boolean isRequiresCommand() {
        return requiresCommand;
    }
}

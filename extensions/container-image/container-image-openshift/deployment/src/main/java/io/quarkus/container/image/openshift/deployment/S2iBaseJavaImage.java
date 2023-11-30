
package io.quarkus.container.image.openshift.deployment;

import java.util.Optional;

import io.quarkus.container.image.deployment.util.ImageUtil;

public enum S2iBaseJavaImage {

    //We only compare `repositories` so registries and tags are stripped
    FABRIC8("fabric8/s2i-java:latest", "JAVA_MAIN_CLASS", "JAVA_APP_JAR", "JAVA_LIB_DIR", "JAVA_CLASSPATH", "JAVA_OPTIONS");

    private final String image;
    private final String javaMainClassEnvVar;
    private final String jarEnvVar;
    private final String jarLibEnvVar;
    private final String classpathEnvVar;
    private final String jvmOptionsEnvVar;

    public static Optional<S2iBaseJavaImage> findMatching(String image) {
        for (S2iBaseJavaImage candidate : S2iBaseJavaImage.values()) {
            if (ImageUtil.getRepository(candidate.getImage()).equals(ImageUtil.getRepository(image))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private S2iBaseJavaImage(String image, String javaMainClassEnvVar, String jarEnvVar, String jarLibEnvVar,
            String classpathEnvVar, String jvmOptionsEnvVar) {
        this.image = image;
        this.javaMainClassEnvVar = javaMainClassEnvVar;
        this.jarEnvVar = jarEnvVar;
        this.jarLibEnvVar = jarLibEnvVar;
        this.classpathEnvVar = classpathEnvVar;
        this.jvmOptionsEnvVar = jvmOptionsEnvVar;
    }

    public String getImage() {
        return image;
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

}

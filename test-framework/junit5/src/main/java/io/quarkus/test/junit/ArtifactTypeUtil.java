package io.quarkus.test.junit;

public final class ArtifactTypeUtil {

    private ArtifactTypeUtil() {
    }

    public static boolean isContainer(String artifactType) {
        return "jar-container".equals(artifactType) || "native-container".equals(artifactType);
    }

    public static boolean isNativeBinary(String artifactType) {
        return "native".equals(artifactType);
    }

    public static boolean isJar(String artifactType) {
        return "jar".equals(artifactType);
    }
}

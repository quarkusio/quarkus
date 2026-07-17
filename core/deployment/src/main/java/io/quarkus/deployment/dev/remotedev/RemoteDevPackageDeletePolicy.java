package io.quarkus.deployment.dev.remotedev;

public final class RemoteDevPackageDeletePolicy {

    private RemoteDevPackageDeletePolicy() {
    }

    public static boolean canDelete(String relativePath) {
        String normalized = normalize(relativePath);
        return normalized.contains("/")
                && !normalized.endsWith("META-INF/MANIFEST.MF")
                && !normalized.contains("META-INF/maven");
    }

    static String normalize(String relativePath) {
        return relativePath.replace('\\', '/');
    }
}

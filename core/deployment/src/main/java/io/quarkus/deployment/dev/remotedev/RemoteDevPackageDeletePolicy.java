package io.quarkus.deployment.dev.remotedev;

/**
 * Conservative lexical policy for deletion requests sent to a remote-development endpoint.
 * <p>
 * The policy accepts only safe, nested package-relative paths and protects package metadata that must not be removed
 * through an incremental difference. It performs no filesystem access and must not be used as a general path
 * containment or authorization check.
 *
 * <p>
 * <strong>API note:</strong>
 * This class is public so Quarkus build-tool integration components can apply the same policy. It is not a
 * user-configurable policy or a general-purpose security API.
 */
public final class RemoteDevPackageDeletePolicy {

    private RemoteDevPackageDeletePolicy() {
    }

    /**
     * Determines whether an incremental remote-development request may delete the supplied package-relative path.
     * Backslashes are normalized before applying the policy. Absolute paths, traversal segments, colon-containing
     * paths, top-level entries, the manifest, and Maven metadata are rejected.
     *
     * @param relativePath non-{@code null} candidate path relative to the package root
     * @return {@code true} when the normalized path may be sent as a deletion
     * @throws NullPointerException if {@code relativePath} is {@code null}
     */
    public static boolean canDelete(String relativePath) {
        String normalized = normalize(relativePath);
        return isSafeRelativePath(normalized)
                && normalized.contains("/")
                && !normalized.endsWith("META-INF/MANIFEST.MF")
                && !normalized.contains("META-INF/maven");
    }

    static String normalize(String relativePath) {
        return relativePath.replace('\\', '/');
    }

    static boolean isSafeRelativePath(String path) {
        if (path.isBlank() || path.startsWith("/") || path.equals("..") || path.startsWith("../")
                || path.endsWith("/..") || path.contains("/../") || path.equals(".") || path.startsWith("./")
                || path.endsWith("/.") || path.contains("/./")) {
            return false;
        }
        return path.indexOf(':') == -1;
    }
}

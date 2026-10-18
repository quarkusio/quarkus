package io.quarkus.bootstrap.resolver.maven.workspace;

/**
 * Thrown if a "Maven CI Friendly Versions" property in the version could not be resolved.
 *
 * @see <a href="https://maven.apache.org/maven-ci-friendly.html">Maven CI Friendly Versions (maven.apache.org)</a>
 */
public class UnresolvedVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static UnresolvedVersionException forGa(String groupId, String artifactId, String rawVersion) {
        return new UnresolvedVersionException(
                "Failed to resolve version '" + rawVersion + "' for " + groupId + ":" + artifactId);
    }

    public UnresolvedVersionException(String message) {
        super(message);
    }
}

package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.BootstrapException;

public class UnresolvedRevisionException extends BootstrapException {

    private static final long serialVersionUID = 1L;

    public static UnresolvedRevisionException forGa(String groupId, String artifactId) {
        return new UnresolvedRevisionException(
                "Failed to resolve " + LocalProject.REVISION_EXPR + " for " + groupId + ":" + artifactId);
    }

    public UnresolvedRevisionException(String message) {
        super(message);
    }
}

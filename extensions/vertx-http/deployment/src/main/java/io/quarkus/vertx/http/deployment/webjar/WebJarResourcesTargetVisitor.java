package io.quarkus.vertx.http.deployment.webjar;

import java.io.IOException;
import java.io.InputStream;

public interface WebJarResourcesTargetVisitor {
    default void visitDirectory(String path) throws IOException {
    }

    default void visitFile(String path, InputStream stream) throws IOException {

    }

    default boolean supportsOnlyCopyingNonArtifactFiles() {
        return false;
    }
}

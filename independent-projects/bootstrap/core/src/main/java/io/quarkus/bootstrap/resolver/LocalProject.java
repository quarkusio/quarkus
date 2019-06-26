package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;

public interface LocalProject extends AutoCloseable {

    /**
     * Create an {@link AppModelResolver} for the application built by this local project.
     */
    AppModelResolver createAppModelResolver() throws AppModelResolverException;

    /**
     * Get the {@link AppArtifact} built by this local project.
     */
    AppArtifact getAppArtifact();

    @Override
    void close() throws BootstrapException;
}

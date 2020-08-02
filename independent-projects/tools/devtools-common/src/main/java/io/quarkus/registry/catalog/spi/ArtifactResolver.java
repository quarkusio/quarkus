package io.quarkus.registry.catalog.spi;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.model.Extension;
import io.quarkus.registry.catalog.model.Platform;
import io.quarkus.registry.model.Release;
import java.io.IOException;

/**
 * Resolves artifacts from the underlying artifact repositories
 */
public interface ArtifactResolver {
    /**
     * Resolve this specific platform
     */
    QuarkusPlatformDescriptor resolvePlatform(Platform platform, Release release) throws IOException;

    /**
     * Resolve this specific extension
     */
    io.quarkus.dependencies.Extension resolveExtension(Extension extension, Release release) throws IOException;
}

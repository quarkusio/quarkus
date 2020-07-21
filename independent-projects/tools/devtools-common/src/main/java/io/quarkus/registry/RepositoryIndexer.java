package io.quarkus.registry;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.model.Extension;
import io.quarkus.registry.catalog.model.Platform;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.catalog.spi.ArtifactResolver;
import io.quarkus.registry.catalog.spi.IndexVisitor;
import io.quarkus.registry.model.Release;
import java.io.IOException;
import java.util.Objects;

/**
 * Indexes a repository
 */
public class RepositoryIndexer {

    private final ArtifactResolver artifactResolver;

    public RepositoryIndexer(ArtifactResolver artifactResolver) {
        this.artifactResolver = Objects.requireNonNull(artifactResolver, "Resolver cannot be null");
    }

    public void index(Repository repository, IndexVisitor visitor) throws IOException {
        // Index Platforms
        for (Platform platform : repository.getPlatforms()) {
            for (Release release : platform.getReleases()) {
                QuarkusPlatformDescriptor descriptor = artifactResolver.resolvePlatform(platform, release);
                if (descriptor != null) {
                    visitor.visitPlatform(descriptor);
                }
            }
        }

        // Index extensions
        for (Extension extension : repository.getIndividualExtensions()) {
            for (Release release : extension.getReleases()) {
                io.quarkus.dependencies.Extension ext = artifactResolver.resolveExtension(extension, release);
                if (ext != null) {
                    visitor.visitExtension(ext, release.getQuarkusCore());
                }
            }
        }
    }
}

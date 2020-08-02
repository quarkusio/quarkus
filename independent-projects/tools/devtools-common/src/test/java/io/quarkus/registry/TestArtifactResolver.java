package io.quarkus.registry;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.model.Platform;
import io.quarkus.registry.catalog.spi.ArtifactResolver;
import io.quarkus.registry.model.Release;
import io.quarkus.test.platform.descriptor.loader.QuarkusTestPlatformDescriptorLoader;
import java.io.IOException;

public class TestArtifactResolver implements ArtifactResolver {

    @Override
    public QuarkusPlatformDescriptor resolvePlatform(Platform platform, Release release) {
        final QuarkusTestPlatformDescriptorLoader loader = new QuarkusTestPlatformDescriptorLoader();
        loader.setGroupId(platform.getGroupId());
        loader.setArtifactId(platform.getArtifactId());
        loader.setVersion(release.getVersion());
        return loader.load(null);
    }

    @Override
    public Extension resolveExtension(io.quarkus.registry.catalog.model.Extension extension, Release release)
            throws IOException {
        return new Extension(extension.getGroupId(), extension.getArtifactId(), release.getVersion());
    }
}

package io.quarkus.registry;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.registry.catalog.model.Platform;
import io.quarkus.registry.catalog.spi.ArtifactResolver;
import io.quarkus.registry.model.Release;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.maven.model.Dependency;

public class TestArtifactResolver implements ArtifactResolver {

    @Override
    public QuarkusPlatformDescriptor resolvePlatform(Platform platform, Release release) throws IOException {
        return new QuarkusPlatformDescriptor() {

            @Override
            public String getBomGroupId() {
                return platform.getGroupId();
            }

            @Override
            public String getBomArtifactId() {
                return platform.getArtifactId();
            }

            @Override
            public String getBomVersion() {
                return release.getVersion();
            }

            @Override
            public String getQuarkusVersion() {
                return Objects.toString(release.getQuarkusCore(), "1.0.0");
            }

            @Override
            public List<Dependency> getManagedDependencies() {
                return Collections.emptyList();
            }

            @Override
            public List<Extension> getExtensions() {
                return Collections.emptyList();
            }

            @Override
            public List<Category> getCategories() {
                return Collections.emptyList();
            }

            @Override
            public String getTemplate(String name) {
                return null;
            }

            @Override
            public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
                return null;
            }
        };
    }

    @Override
    public Extension resolveExtension(io.quarkus.registry.catalog.model.Extension extension, Release release)
            throws IOException {
        return new Extension(extension.getGroupId(), extension.getArtifactId(), release.getVersion());
    }
}

package io.quarkus.registry.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.model.Extension;
import io.quarkus.registry.catalog.model.Platform;
import io.quarkus.registry.model.Release;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class DefaultArtifactResolverTest {

    private DefaultArtifactResolver resolver = new DefaultArtifactResolver();

    @Test
    void shouldFormatURL() {
        Release release = Release.builder().version("1.3.1.Final").build();
        Platform platform = Platform.builder()
                .groupId("io.quarkus")
                .artifactId("quarkus-universe-bom")
                .addReleases(release).build();
        URL url = DefaultArtifactResolver.getPlatformJSONURL(platform, release);
        assertThat(url).hasPath("/maven2/io/quarkus/quarkus-universe-bom/1.3.1.Final/quarkus-universe-bom-1.3.1.Final.json");
    }

    @Test
    void shouldResolvePlatform() throws IOException {
        Release release = Release.builder().version("1.3.1.Final").build();
        Platform platform = Platform.builder()
                .groupId("io.quarkus")
                .artifactId("quarkus-universe-bom")
                .addReleases(release).build();
        QuarkusPlatformDescriptor descriptor = resolver.resolvePlatform(platform, release);
        assertThat(descriptor).isNotNull();
    }

    @Test
    void shouldResolveExtension() throws IOException {
        Release release = Release.builder().version("1.3.1.Final").build();
        Extension extension = Extension.builder()
                .groupId("io.quarkus")
                .artifactId("quarkus-jgit")
                .addReleases(release)
                .build();
        io.quarkus.dependencies.Extension ext = resolver.resolveExtension(extension, release);
        assertThat(ext).isNotNull();
        assertThat(ext.getArtifactId()).isEqualTo("quarkus-jgit");
    }
}

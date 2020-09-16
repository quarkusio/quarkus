package io.quarkus.registry.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.registry.ExtensionRegistry;
import io.quarkus.registry.catalog.model.Extension;
import io.quarkus.registry.catalog.spi.ArtifactResolver;
import io.quarkus.registry.model.Release;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ExtensionRegistryBuilderTest {

    @Test
    void shouldResolveCamelDependencies() throws IOException {
        String groupId = "org.apache.camel";
        String artifactId = "camel-quarkus";
        String version = "1.6.0.Final";

        ArtifactResolver artifactResolver = mock(ArtifactResolver.class);
        Release release = Release.builder().version(version).build();
        Extension extension = Extension.builder().groupId(groupId).artifactId(artifactId)
                .addReleases(release).build();
        when(artifactResolver.resolveExtension(extension, release))
                .thenReturn(new io.quarkus.dependencies.Extension(groupId, artifactId, version));

        ExtensionRegistry registry = new ExtensionRegistryBuilder(artifactResolver)
                .addExtension(groupId, artifactId, version, version)
                .build();
        verify(artifactResolver, atMostOnce()).resolveExtension(extension, release);
        assertThat(registry.list(version, "camel")).isNotEmpty();
    }

}

package io.quarkus.registry.client.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import org.junit.jupiter.api.Test;

public class MavenRegistryClientCompleteConfigTest {

    @Test
    public void testCompletePlatformsConfig() throws Exception {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable originalRegistryConfig = RegistryConfig.builder();
        originalRegistryConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setExtensionCatalogsIncluded(true)
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(originalRegistryConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isTrue();
    }
}

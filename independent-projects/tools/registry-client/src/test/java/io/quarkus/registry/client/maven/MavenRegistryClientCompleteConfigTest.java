package io.quarkus.registry.client.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryMavenRepoConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;

public class MavenRegistryClientCompleteConfigTest {

    @Test
    public void testCompletePlatformsConfig() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final Map<String, Object> offerings = Map.of("offerings", List.of("quarkus", "camel-quarkus"));
        final RegistryConfig.Mutable originalRegistryConfig = RegistryConfig.builder();
        originalRegistryConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build())
                .setExtra(offerings);

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
        assertThat(completeConfig.getExtra()).isEqualTo(offerings);
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isTrue();
    }

    @Test
    public void testPlatformExtensionsMavenConfigProvidedByRegistry() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable localClientConfig = RegistryConfig.builder();
        localClientConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("repo-id")
                                        .setUrl("repo-url")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(localClientConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isNull();
        assertThat(completePlatforms.getMaven()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository().getId()).isEqualTo("repo-id");
        assertThat(completePlatforms.getMaven().getRepository().getUrl()).isEqualTo("repo-url");
    }

    @Test
    public void testClientOverridingPlatformExtensionsMavenConfigProvidedByRegistry() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable localClientConfig = RegistryConfig.builder();
        localClientConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("client-repo-id")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("repo-id")
                                        .setUrl("repo-url")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(localClientConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isNull();
        assertThat(completePlatforms.getMaven()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository().getId()).isEqualTo("client-repo-id");
        assertThat(completePlatforms.getMaven().getRepository().getUrl()).isEqualTo("repo-url");
    }

    @Test
    public void testPlatformExtensionsMavenConfigProvidedByClient() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable localClientConfig = RegistryConfig.builder();
        localClientConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("repo-id")
                                        .setUrl("repo-url")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(localClientConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isNull();
        assertThat(completePlatforms.getMaven()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository().getId()).isEqualTo("repo-id");
        assertThat(completePlatforms.getMaven().getRepository().getUrl()).isEqualTo("repo-url");
    }

    @Test
    public void testPlatformExtensionsMavenConfigProvidedByClientOverridesRegistryIncludedCatalogs() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable localClientConfig = RegistryConfig.builder();
        localClientConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("repo-id")
                                        .setUrl("repo-url")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setExtensionCatalogsIncluded(true)
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(localClientConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isNull();
        assertThat(completePlatforms.getMaven()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository()).isNotNull();
        assertThat(completePlatforms.getMaven().getRepository().getId()).isEqualTo("repo-id");
        assertThat(completePlatforms.getMaven().getRepository().getUrl()).isEqualTo("repo-url");
    }

    @Test
    public void testClientPlatformExtensionIncludedCatalogsOverridesRegistryMavenConfig() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable localClientConfig = RegistryConfig.builder();
        localClientConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setExtensionCatalogsIncluded(true)
                        .build());

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setId("repo-id")
                                        .setUrl("repo-url")
                                        .build())
                                .build())
                        .build());

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(localClientConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completePlatforms.getExtensionCatalogsIncluded()).isTrue();
        assertThat(completePlatforms.getMaven()).isNull();
    }

    @Test
    public void testRecognizedVersionOverride() {

        final RegistryDescriptorConfig descriptorConfig = RegistryDescriptorConfig.builder()
                .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"))
                .build();

        final RegistryConfig.Mutable originalRegistryConfig = RegistryConfig.builder();
        originalRegistryConfig.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build())
                .setQuarkusVersions(RegistryQuarkusVersionsConfig.builder()
                        .setRecognizedVersionsExpression("*acme-acme*")
                        .setExclusiveProvider(true));

        final RegistryConfig.Mutable registryDescriptor = RegistryConfig.builder();
        registryDescriptor.setId("acme-registry")
                .setDescriptor(descriptorConfig)
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"))
                        .build())
                .setQuarkusVersions(RegistryQuarkusVersionsConfig.builder()
                        .setRecognizedGroupIds(List.of("org.acme"))
                        .setRecognizedVersionsExpression("*acme*"));

        final RegistryConfig.Mutable completeConfig = MavenRegistryClientFactory.completeRegistryConfig(originalRegistryConfig,
                registryDescriptor);
        assertThat(completeConfig.getId()).isEqualTo("acme-registry");
        assertThat(completeConfig.getDescriptor().getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-registry-descriptor::json:1.0-SNAPSHOT"));
        final RegistryPlatformsConfig completePlatforms = completeConfig.getPlatforms();
        assertThat(completePlatforms).isNotNull();
        assertThat(completePlatforms.getArtifact())
                .isEqualTo(ArtifactCoords.fromString("org.acme.registry:acme-platforms::json:1.0-SNAPSHOT"));
        assertThat(completeConfig.getQuarkusVersions())
                .isEqualTo(RegistryQuarkusVersionsConfig.builder()
                        .setRecognizedVersionsExpression("*acme-acme*")
                        .setExclusiveProvider(true)
                        .setRecognizedGroupIds(List.of("org.acme")));
    }
}

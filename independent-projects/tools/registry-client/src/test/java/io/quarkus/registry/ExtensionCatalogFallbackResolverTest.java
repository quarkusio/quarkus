package io.quarkus.registry;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigImpl;
import io.quarkus.registry.config.RegistryConfigImpl;
import io.quarkus.registry.config.RegistryPlatformsConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtensionCatalogFallbackResolverTest {
    static final String quarkusBomVersion = System.getProperty("project.version");
    static final String quarkusBomGroupId = System.getProperty("project.groupId");

    final MessageWriter log = MessageWriter.debug();
    final RegistriesConfig testConfig = RegistriesConfigImpl.builder()
            .withDebug(true)
            .withRegistry(RegistryConfigImpl.builder()
                    .withId("test.quarkus.registry")
                    .withPlatforms(RegistryPlatformsConfigImpl.builder()
                            .withArtifact(new ArtifactCoords("registry.quarkus.test",
                                    Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                                    "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())
                    .build())
            .build();

    ExtensionCatalogResolver.Builder builder;
    ExtensionCatalogFallbackResolver fallbackResolver;

    @BeforeEach
    void setUp() {
        if (quarkusBomVersion == null) {
            throw new IllegalStateException("quarkusBomVersion isn't set");
        }
        if (quarkusBomGroupId == null) {
            throw new IllegalStateException("quarkusBomGroupId isn't set");
        }

        builder = ExtensionCatalogResolver.builder()
                .withLog(log)
                .withConfig(testConfig);
    }

    @Test
    void resolveExtensionCatalog() {
    }

    @Test
    void testResolveExtensionCatalog() {
    }

    @Test
    void testResolveExtensionCatalog1() {
    }

    @Test
    void testResolveExtensionCatalog2() {
    }

    @Test
    void resolvePlatformCatalog() {
    }

    @Test
    void testResolvePlatformCatalog() {
    }

    @Test
    void resolvePlatformCatalogFromRegistry() {
    }

    @Test
    void testResolvePlatformCatalogFromRegistry() {
    }

    @Test
    void findPlatformJson() {
    }
}

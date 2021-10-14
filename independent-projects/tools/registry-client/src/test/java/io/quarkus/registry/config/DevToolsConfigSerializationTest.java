package io.quarkus.registry.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * TODO: compare with *.json.DevToolsConfigSerializationTest
 */
public class DevToolsConfigSerializationTest {
    @Test
    public void testIdOnly() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry("registry.quarkus.io")
                .withRegistry("registry.other.org")
                .build();

        final String configName = "registry-id-only.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testGlobalDebugEnabled() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withDebug(true)
                .withRegistry("registry.quarkus.io")
                .build();

        final String configName = "registry-id-only-debug.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryUpdatePolicy() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withUpdatePolicy("always")
                        .build())
                .build();

        final String configName = "registry-update-policy.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDisabled() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withEnabled(false)
                        .build())
                .build();
        // no registries are enabled, the default registry should be added by default

        final String configName = "registry-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDescriptor() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withDescriptor(RegistryDescriptorConfigImpl.builder()
                                .withArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-registry-descriptor::json:2.0"))
                                .build())
                        .build())
                .build();

        final String configName = "registry-descriptor.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsArtifact() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withPlatforms(RegistryPlatformsConfigImpl.builder()
                                .withArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"))
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-artifact.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsDisabled() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withPlatforms(RegistryPlatformsConfigImpl.builder()
                                .withDisabled(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsExtensionCatalogIncluded() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withPlatforms(RegistryPlatformsConfigImpl.builder()
                                .withArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"))
                                .withExtensionCatalogsIncluded(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-extension-catalog-included.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryNonPlatformExtensionsArtifact() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withNonPlatformExtensions(RegistryNonPlatformExtensionsConfigImpl.builder()
                                .withArtifact(
                                        ArtifactCoords.fromString("org.acme:acme-quarkus-non-platform-extensions::json:2.0"))
                                .build())
                        .build())
                .build();

        final String configName = "registry-non-platform-extensions-artifact.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryNonPlatformExtensionsDisabled() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withNonPlatformExtensions(RegistryNonPlatformExtensionsConfigImpl.builder()
                                .withDisabled(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-non-platform-extensions-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryMavenRepoUrl() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withMaven(RegistryMavenConfigImpl.builder()
                                .withRepository(RegistryMavenRepoConfigImpl.builder()
                                        .withUrl("https://repo.acme.org/maven")
                                        .build())
                                .build())
                        .build())
                .build();

        final String configName = "registry-maven-repo-url.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryMavenRepoUrlAndId() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withMaven(RegistryMavenConfigImpl.builder()
                                .withRepository(RegistryMavenRepoConfigImpl.builder()
                                        .withUrl("https://repo.acme.org/maven")
                                        .withId("acme-repo")
                                        .build())
                                .build())
                        .build())
                .build();

        final String configName = "registry-maven-repo-url-id.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryRecognizedQuarkusVersions() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withQuarkusVersions(RegistryQuarkusVersionsConfigImpl.builder()
                                .withRecognizedVersionsExpression("*-acme-*")
                                .build())
                        .build())
                .build();

        final String configName = "registry-recognized-quarkus-versions.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryQuarkusVersionsExclusiveProvider() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withQuarkusVersions(RegistryQuarkusVersionsConfigImpl.builder()
                                .withRecognizedVersionsExpression("*-acme-*")
                                .withExclusiveProvider(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-quarkus-versions-exclusive-provider.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnySimpleProperty() throws Exception {
        final RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withExtra("client-factory-artifact", "org.acme:acme-registry-client-factory::jar:2.0")
                        .build())
                .build();

        final String configName = "registry-any-simple-property.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnyCustomObject() throws Exception {
        RegistriesConfig config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withExtra("custom", new Custom("value"))
                        .build())
                .build();

        final String configName = "registry-any-custom-object.yaml";
        assertSerializedMatches(config, configName);

        config = RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("registry.acme.org")
                        .withExtra("custom", Collections.singletonMap("prop", "value"))
                        .build())
                .build();

        assertDeserializedMatches(configName, config);
    }

    private static void assertSerializedMatches(RegistriesConfig config, String configName) throws Exception {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            RegistriesConfigMapperHelper.toYaml(config, writer);
        }
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(buf.getBuffer().toString()))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        }
        List<String> expected = Files.readAllLines(resolveConfigPath(configName));
        assertThat(lines).isEqualTo(expected);
    }

    private static void assertDeserializedMatches(String configName, RegistriesConfig expected) throws Exception {
        RegistriesConfig actual = RegistriesConfigMapperHelper.deserialize(resolveConfigPath(configName),
                RegistriesConfigImpl.class);
        assertThat(actual).isEqualTo(expected);
    }

    private static Path resolveConfigPath(String configName) throws URISyntaxException {
        final URL configUrl = Thread.currentThread().getContextClassLoader().getResource("devtools-config/" + configName);
        assertThat(configUrl).isNotNull();
        final Path path = Paths.get(configUrl.toURI());
        return path;
    }

    public static class Custom {

        public final String prop;

        public Custom(String prop) {
            this.prop = prop;
        }

        @Override
        public int hashCode() {
            return Objects.hash(prop);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Custom other = (Custom) obj;
            return Objects.equals(prop, other.prop);
        }

        @Override
        public String toString() {
            return "prop=" + prop;
        }
    }
}

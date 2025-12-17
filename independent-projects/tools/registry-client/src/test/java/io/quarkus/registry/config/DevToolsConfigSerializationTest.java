package io.quarkus.registry.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * TODO: compare set *.json.DevToolsConfigSerializationTest
 */
public class DevToolsConfigSerializationTest {
    static Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("src/test/resources/devtools-config");
    static Path writeDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-serialization");

    @Test
    public void testReadWriteDefaultEmptyConfig() throws Exception {
        Path output = writeDir.resolve("registry-default-only.yaml");

        RegistriesConfig.Mutable config;
        RegistriesConfig actual;
        String contents;

        config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.defaultConfig());
        config.persist(output); // MutableRegistriesConfig

        contents = Files.readString(output);
        assertThat(contents).isEqualTo("---\n");

        actual = RegistriesConfigMapperHelper.deserialize(output, RegistriesConfigImpl.class);
        assertThat(actual).isNull();
        actual = RegistriesConfigLocator.load(output);
        assertThat(actual).isEqualTo(config.build());

        // Emit debug parameter, but no registries
        config = RegistriesConfig.builder()
                .setDebug(true)
                .setRegistry(RegistryConfig.defaultConfig());
        config.persist(output); // MutableRegistriesConfig

        contents = Files.readString(output);
        assertThat(contents).isEqualTo("---\ndebug: true\n");

        actual = RegistriesConfig.fromFile(output);
        assertThat(actual).isEqualTo(config.build());
        actual = RegistriesConfigLocator.load(output);
        assertThat(actual).isEqualTo(config.build());
    }

    @Test
    public void testIdOnly() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry("registry.quarkus.io")
                .setRegistry("registry.other.org")
                .build();

        final String configName = "registry-id-only.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testGlobalDebugEnabled() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setDebug(true)
                .setRegistry("registry.quarkus.io")
                .build();

        final String configName = "registry-id-only-debug.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryUpdatePolicy() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setUpdatePolicy("always")
                        .build())
                .build();

        final String configName = "registry-update-policy.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDisabled() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setEnabled(false)
                        .build())
                .build();
        // no registries are enabled, the default registry should be added by default

        final String configName = "registry-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDescriptor() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-registry-descriptor::json:2.0"))
                                .build())
                        .build())
                .build();

        final String configName = "registry-descriptor.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsArtifact() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setPlatforms(RegistryPlatformsConfig.builder()
                                .setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"))
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-artifact.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsDisabled() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setPlatforms(RegistryPlatformsConfig.builder()
                                .setDisabled(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsExtensionCatalogIncluded() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setPlatforms(RegistryPlatformsConfig.builder()
                                .setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"))
                                .setExtensionCatalogsIncluded(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-platforms-extension-catalog-included.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryNonPlatformExtensionsArtifact() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig.builder()
                                .setArtifact(
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
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig.builder()
                                .setDisabled(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-non-platform-extensions-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryMavenRepoUrl() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setUrl("https://repo.acme.org/maven")
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
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setUrl("https://repo.acme.org/maven")
                                        .setId("acme-repo")
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
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setQuarkusVersions(RegistryQuarkusVersionsConfig.builder()
                                .setRecognizedVersionsExpression("*-acme-*")
                                .build())
                        .build())
                .build();

        final String configName = "registry-recognized-quarkus-versions.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryQuarkusVersionsExclusiveProvider() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setQuarkusVersions(RegistryQuarkusVersionsConfig.builder()
                                .setRecognizedVersionsExpression("*-acme-*")
                                .setExclusiveProvider(true)
                                .build())
                        .build())
                .build();

        final String configName = "registry-quarkus-versions-exclusive-provider.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnySimpleProperty() throws Exception {
        final RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setExtra("client-factory-artifact", "org.acme:acme-registry-client-factory::jar:2.0")
                        .build())
                .build();

        final String configName = "registry-any-simple-property.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnyCustomObject() throws Exception {
        RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setExtra("custom", new Custom("value"))
                        .build())
                .build();

        final String configName = "registry-any-custom-object.yaml";
        assertSerializedMatches(config, configName);

        config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setExtra("custom", Collections.singletonMap("prop", "value"))
                        .build())
                .build();

        assertDeserializedMatches(configName, config);
    }

    @Test
    void testRegistryClientJsonConfig() throws IOException {
        String configName = "registry-client-config.json";
        Path expectedFile = baseDir.resolve(configName);
        Path actualFile = writeDir.resolve(configName);

        // Don't build any of these bits... let persist do it..
        RegistryConfig expected = RegistryConfig.builder()
                .setId("registry.acme.org")
                .setDescriptor(RegistryDescriptorConfig.builder()
                        .setArtifact(
                                ArtifactCoords
                                        .fromString("registry.quarkus.test:quarkus-registry-descriptor::json:1.0-SNAPSHOT")))
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("registry.quarkus.test:quarkus-platforms::json:1.0-SNAPSHOT"))
                        .setExtensionCatalogsIncluded(true))
                .setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig.builder()
                        .setDisabled(true)
                        .setArtifact(ArtifactCoords
                                .fromString("registry.quarkus.test:quarkus-non-platform-extensions::json:1.0-SNAPSHOT")));

        expected.persist(actualFile);

        String expectedContents = Files.readString(expectedFile);
        String actualContents = Files.readString(actualFile);

        assertThat(actualContents).isEqualTo(expectedContents);
    }

    @Test
    void testRegistryClientMavenPlatformExtensionJsonConfig() throws IOException {
        String configName = "registry-client-platform-extension-maven-config.json";
        Path expectedFile = baseDir.resolve(configName);
        Path actualFile = writeDir.resolve(configName);

        RegistryConfig expected = RegistryConfig.builder()
                .setId("registry.acme.org")
                .setDescriptor(RegistryDescriptorConfig.builder()
                        .setArtifact(
                                ArtifactCoords
                                        .fromString("registry.quarkus.test:quarkus-registry-descriptor::json:1.0-SNAPSHOT")))
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("registry.quarkus.test:quarkus-platforms::json:1.0-SNAPSHOT"))
                        .setMaven(RegistryMavenConfig.builder().setRepository(
                                RegistryMavenRepoConfig.builder().setId("repo-id").setUrl("repo-url"))))
                .setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig.builder()
                        .setDisabled(true)
                        .setArtifact(ArtifactCoords
                                .fromString("registry.quarkus.test:quarkus-non-platform-extensions::json:1.0-SNAPSHOT")));

        expected.persist(actualFile);

        String expectedContents = Files.readString(expectedFile);
        String actualContents = Files.readString(actualFile);

        assertThat(actualContents).isEqualTo(expectedContents);
    }

    @Test
    void testReadJsonRegistryDescriptor() throws IOException {
        String configName = "registry-descriptor-1.0-SNAPSHOT.json";
        Path expectedFile = baseDir.resolve(configName);
        Path actualFile = writeDir.resolve(configName);

        RegistryConfig expected = RegistryConfig.builder()
                .setId("registry.foo.org")
                .setDescriptor(RegistryDescriptorConfig.builder()
                        .setArtifact(
                                ArtifactCoords
                                        .fromString("org.foo.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT")))
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(
                                ArtifactCoords
                                        .fromString("org.foo.registry:quarkus-registry-platforms::json:1.0-SNAPSHOT")))
                .build();

        expected.persist(actualFile);

        String expectedContents = Files.readString(expectedFile);
        String actualContents = Files.readString(actualFile);
        assertThat(actualContents).isEqualTo(expectedContents);

        RegistryConfig.Mutable actual = RegistryConfig.mutableFromFile(actualFile);
        actual.setId("registry.foo.org");
        assertThat(actual.build()).isEqualTo(expected);
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
        List<String> expected = Files.readAllLines(baseDir.resolve(configName));
        assertThat(lines).isEqualTo(expected);
    }

    private static void assertDeserializedMatches(String configName, RegistriesConfig expected) throws Exception {
        RegistriesConfig actual = RegistriesConfig.fromFile(baseDir.resolve(configName));
        assertThat(actual).isEqualTo(expected);
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

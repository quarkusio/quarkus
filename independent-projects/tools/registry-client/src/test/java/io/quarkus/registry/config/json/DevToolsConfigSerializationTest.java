package io.quarkus.registry.config.json;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
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
 * TODO: remove me
 * Tests the JsonRegistr*Config classes
 */
@Deprecated
public class DevToolsConfigSerializationTest {

    @Test
    public void testIdOnly() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        config.addRegistry(new JsonRegistryConfig("registry.quarkus.io"));
        config.addRegistry(new JsonRegistryConfig("registry.other.org"));

        final String configName = "registry-id-only.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testGlobalDebugEnabled() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        config.setDebug(true);
        config.addRegistry(new JsonRegistryConfig("registry.quarkus.io"));

        final String configName = "registry-id-only-debug.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryUpdatePolicy() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);
        registry.setUpdatePolicy("always");

        final String configName = "registry-update-policy.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDisabled() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);
        registry.setEnabled(false);
        config.addRegistry(new JsonRegistryConfig("registry.quarkus.io"));

        final String configName = "registry-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryDescriptor() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryDescriptorConfig descr = new JsonRegistryDescriptorConfig();
        descr.setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-registry-descriptor::json:2.0"));
        registry.setDescriptor(descr);

        final String configName = "registry-descriptor.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsArtifact() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryPlatformsConfig platforms = new JsonRegistryPlatformsConfig();
        registry.setPlatforms(platforms);
        platforms.setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"));

        final String configName = "registry-platforms-artifact.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsDisabled() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryPlatformsConfig platforms = new JsonRegistryPlatformsConfig();
        registry.setPlatforms(platforms);
        platforms.setDisabled(true);

        final String configName = "registry-platforms-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryPlatformsExtensionCatalogIncluded() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryPlatformsConfig platforms = new JsonRegistryPlatformsConfig();
        registry.setPlatforms(platforms);
        platforms.setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-platforms::json:2.0"));
        platforms.setExtensionCatalogsIncluded(true);

        final String configName = "registry-platforms-extension-catalog-included.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryNonPlatformExtensionsArtifact() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryNonPlatformExtensionsConfig nonPlatformExtensions = new JsonRegistryNonPlatformExtensionsConfig();
        registry.setNonPlatformExtensions(nonPlatformExtensions);
        nonPlatformExtensions.setArtifact(ArtifactCoords.fromString("org.acme:acme-quarkus-non-platform-extensions::json:2.0"));

        final String configName = "registry-non-platform-extensions-artifact.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryNonPlatformExtensionsDisabled() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryNonPlatformExtensionsConfig nonPlatformExtensions = new JsonRegistryNonPlatformExtensionsConfig();
        registry.setNonPlatformExtensions(nonPlatformExtensions);
        nonPlatformExtensions.setDisabled(true);

        final String configName = "registry-non-platform-extensions-disabled.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryMavenRepoUrl() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryMavenConfig maven = new JsonRegistryMavenConfig();
        registry.setMaven(maven);

        final JsonRegistryMavenRepoConfig repo = new JsonRegistryMavenRepoConfig();
        maven.setRepository(repo);
        repo.setUrl("https://repo.acme.org/maven");

        final String configName = "registry-maven-repo-url.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryMavenRepoUrlAndId() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryMavenConfig maven = new JsonRegistryMavenConfig();
        registry.setMaven(maven);

        final JsonRegistryMavenRepoConfig repo = new JsonRegistryMavenRepoConfig();
        maven.setRepository(repo);
        repo.setUrl("https://repo.acme.org/maven");
        repo.setId("acme-repo");

        final String configName = "registry-maven-repo-url-id.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryRecognizedQuarkusVersions() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryQuarkusVersionsConfig versions = new JsonRegistryQuarkusVersionsConfig();
        registry.setQuarkusVersions(versions);
        versions.setRecognizedVersionsExpression("*-acme-*");

        final String configName = "registry-recognized-quarkus-versions.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryQuarkusVersionsExclusiveProvider() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        final JsonRegistryQuarkusVersionsConfig versions = new JsonRegistryQuarkusVersionsConfig();
        registry.setQuarkusVersions(versions);
        versions.setRecognizedVersionsExpression("*-acme-*");
        versions.setExclusiveProvider(true);

        final String configName = "registry-quarkus-versions-exclusive-provider.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnySimpleProperty() throws Exception {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        registry.setAny("client-factory-artifact", "org.acme:acme-registry-client-factory::jar:2.0");

        final String configName = "registry-any-simple-property.yaml";
        assertDeserializedMatches(configName, config);
        assertSerializedMatches(config, configName);
    }

    @Test
    public void testRegistryAnyCustomObject() throws Exception {
        JsonRegistriesConfig config = new JsonRegistriesConfig();
        JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);

        registry.setAny("custom", new Custom("value"));

        final String configName = "registry-any-custom-object.yaml";
        assertSerializedMatches(config, configName);

        config = new JsonRegistriesConfig();
        registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);
        registry.setAny("custom", Collections.singletonMap("prop", "value"));

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
        assertThat(lines).isEqualTo(Files.readAllLines(resolveConfigPath(configName)));
    }

    private static void assertDeserializedMatches(String configName, RegistriesConfig config) throws Exception {
        assertThat(RegistriesConfigMapperHelper.deserialize(resolveConfigPath(configName), JsonRegistriesConfig.class))
                .isEqualTo(config);
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

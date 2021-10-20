package io.quarkus.registry.config.json;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * TODO: remove me
 * Tests the old/previous locator now in the .json package
 */
@Deprecated
public class JsonRegistriesConfigLocatorTest {

    @Test
    void configCompletion() throws Exception {

        final io.quarkus.registry.config.json.JsonRegistriesConfig config = new io.quarkus.registry.config.json.JsonRegistriesConfig();
        final io.quarkus.registry.config.json.JsonRegistryConfig registry = new io.quarkus.registry.config.json.JsonRegistryConfig(
                "registry.acme.org");
        config.addRegistry(registry);
        registry.setUpdatePolicy("always");

        final RegistriesConfig completeConfig = serializeDeserialize(config);
        assertThat(completeConfig.getRegistries().size()).isEqualTo(1);
        final RegistryConfig completeRegistry = completeConfig.getRegistries().get(0);
        assertThat(completeRegistry.getId()).isEqualTo("registry.acme.org");
        assertThat(completeRegistry.getUpdatePolicy()).isEqualTo("always");
        final RegistryMavenConfig mavenConfig = completeRegistry.getMaven();
        assertThat(mavenConfig).isNull();
    }

    @Test
    void testSimpleRegistryListFromEnvironment() {
        final Map<String, String> env = Collections.singletonMap(
                io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRIES,
                "registry.acme.org,registry.other.io");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final io.quarkus.registry.config.json.JsonRegistriesConfig expectedConfig = new io.quarkus.registry.config.json.JsonRegistriesConfig();

        io.quarkus.registry.config.json.JsonRegistryConfig registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.acme.org");
        io.quarkus.registry.config.json.JsonRegistryDescriptorConfig descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryUpdatePolicyFromEnvironment() {
        final Map<String, String> env = new HashMap<>();
        env.put(io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRIES,
                "registry.acme.org,registry.other.io");
        env.put(io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX
                + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final io.quarkus.registry.config.json.JsonRegistriesConfig expectedConfig = new io.quarkus.registry.config.json.JsonRegistriesConfig();

        io.quarkus.registry.config.json.JsonRegistryConfig registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.acme.org");
        io.quarkus.registry.config.json.JsonRegistryDescriptorConfig descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        registry.setUpdatePolicy("always");
        expectedConfig.addRegistry(registry);

        registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryRepositoryURLFromEnvironment() {
        final Map<String, String> env = new HashMap<>();
        env.put(io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRIES,
                "registry.acme.org,registry.other.io");
        env.put(io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX
                + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");
        env.put(io.quarkus.registry.config.json.JsonRegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX
                + "REGISTRY_OTHER_IO_REPO_URL",
                "https://custom.registry.net/mvn");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final io.quarkus.registry.config.json.JsonRegistriesConfig expectedConfig = new io.quarkus.registry.config.json.JsonRegistriesConfig();

        io.quarkus.registry.config.json.JsonRegistryConfig registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.acme.org");
        io.quarkus.registry.config.json.JsonRegistryDescriptorConfig descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        registry.setUpdatePolicy("always");
        expectedConfig.addRegistry(registry);

        registry = new io.quarkus.registry.config.json.JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new io.quarkus.registry.config.json.JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        io.quarkus.registry.config.json.JsonRegistryMavenConfig maven = new io.quarkus.registry.config.json.JsonRegistryMavenConfig();
        registry.setMaven(maven);
        io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig repo = new io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig();
        maven.setRepository(repo);
        repo.setUrl("https://custom.registry.net/mvn");
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    private static RegistriesConfig initFromEnvironment(Map<String, String> env) {
        return io.quarkus.registry.config.json.JsonRegistriesConfigLocator.initFromEnvironmentOrNull(env);
    }

    private static RegistriesConfig serializeDeserialize(RegistriesConfig config) throws Exception {
        final StringWriter buf = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(config, buf);
        try (StringReader reader = new StringReader(buf.toString())) {
            return io.quarkus.registry.config.json.JsonRegistriesConfigLocator.load(reader);
        }
    }
}

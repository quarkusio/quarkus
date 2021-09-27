package io.quarkus.registry.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RegistriesConfigLocatorTest {

    @Test
    void configCompletion() throws Exception {

        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
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
        final Map<String, String> env = Collections.singletonMap(RegistriesConfigLocator.QUARKUS_REGISTRIES,
                "registry.acme.org,registry.other.io");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final JsonRegistriesConfig expectedConfig = new JsonRegistriesConfig();

        JsonRegistryConfig registry = new JsonRegistryConfig();
        registry.setId("registry.acme.org");
        JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        registry = new JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryUpdatePolicyFromEnvironment() {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final JsonRegistriesConfig expectedConfig = new JsonRegistriesConfig();

        JsonRegistryConfig registry = new JsonRegistryConfig();
        registry.setId("registry.acme.org");
        JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        registry.setUpdatePolicy("always");
        expectedConfig.addRegistry(registry);

        registry = new JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryRepositoryURLFromEnvironment() {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_OTHER_IO_REPO_URL",
                "https://custom.registry.net/mvn");
        final RegistriesConfig actualConfig = initFromEnvironment(env);

        final JsonRegistriesConfig expectedConfig = new JsonRegistriesConfig();

        JsonRegistryConfig registry = new JsonRegistryConfig();
        registry.setId("registry.acme.org");
        JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        registry.setUpdatePolicy("always");
        expectedConfig.addRegistry(registry);

        registry = new JsonRegistryConfig();
        registry.setId("registry.other.io");
        descriptor = new JsonRegistryDescriptorConfig();
        descriptor.setArtifact(ArtifactCoords.fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"));
        registry.setDescriptor(descriptor);
        JsonRegistryMavenConfig maven = new JsonRegistryMavenConfig();
        registry.setMaven(maven);
        JsonRegistryMavenRepoConfig repo = new JsonRegistryMavenRepoConfig();
        maven.setRepository(repo);
        repo.setUrl("https://custom.registry.net/mvn");
        expectedConfig.addRegistry(registry);

        assertThat(actualConfig).isEqualTo(expectedConfig);
    }

    private static RegistriesConfig initFromEnvironment(Map<String, String> env) {
        return RegistriesConfigLocator.initFromEnvironmentOrNull(env);
    }

    private static RegistriesConfig serializeDeserialize(RegistriesConfig config) throws Exception {
        final StringWriter buf = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(config, buf);
        try (StringReader reader = new StringReader(buf.toString())) {
            return RegistriesConfigLocator.load(reader);
        }
    }
}

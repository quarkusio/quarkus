package io.quarkus.registry.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;

public class RegistriesConfigLocatorTest {

    @Test
    void configCompletion() throws Exception {
        RegistriesConfig config = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setUpdatePolicy("always")
                        .build())
                .build();

        final RegistriesConfig completeConfig = serializeDeserialize(config);
        assertThat(completeConfig.getRegistries().size()).isEqualTo(1);

        final RegistryConfig completeRegistry = completeConfig.getRegistries().get(0);
        assertThat(completeRegistry.getId()).isEqualTo("registry.acme.org");
        assertThat(completeRegistry.getUpdatePolicy()).isEqualTo("always");

        final RegistryMavenConfig mavenConfig = completeRegistry.getMaven();
        assertThat(mavenConfig).isNull();
    }

    @Test
    void testSimpleRegistryListFromEnvironment() throws Exception {
        final Map<String, String> env = Map.of(RegistriesConfigLocator.QUARKUS_REGISTRIES,
                "registry.acme.org,registry.other.io");

        final RegistriesConfig actualConfig = RegistriesConfigLocator.initFromEnvironmentOrNull(env);

        final RegistriesConfig expectedConfig = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .build())
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.other.io")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .build())
                .build();

        assertThat(actualConfig).isEqualTo(expectedConfig);

        final RegistriesConfig completeConfig = serializeDeserialize(actualConfig);
        assertThat(completeConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryUpdatePolicyFromEnvironment() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");

        final RegistriesConfig actualConfig = RegistriesConfigLocator.initFromEnvironmentOrNull(env);

        final RegistriesConfig expectedConfig = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .setUpdatePolicy("always")
                        .build())
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.other.io")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .build())
                .build();

        assertThat(actualConfig).isEqualTo(expectedConfig);

        final RegistriesConfig completeConfig = serializeDeserialize(actualConfig);
        assertThat(completeConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryRepositoryURLFromEnvironment() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_UPDATE_POLICY", "always");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_OTHER_IO_REPO_URL",
                "https://custom.registry.net/mvn");

        final RegistriesConfig actualConfig = RegistriesConfigLocator.initFromEnvironmentOrNull(env);

        final RegistriesConfig expectedConfig = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("org.acme.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .setUpdatePolicy("always")
                        .build())
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.other.io")
                        .setDescriptor(RegistryDescriptorConfig.builder()
                                .setArtifact(ArtifactCoords
                                        .fromString("io.other.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT"))
                                .build())
                        .setMaven(RegistryMavenConfig.builder()
                                .setRepository(RegistryMavenRepoConfig.builder()
                                        .setUrl("https://custom.registry.net/mvn")
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(actualConfig).isEqualTo(expectedConfig);

        final RegistriesConfig completeConfig = serializeDeserialize(actualConfig);
        assertThat(completeConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRegistryOfferingFromEnvironment() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_OFFERING", "acme-magic");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_OTHER_IO_OFFERING", "other-cloud");

        final RegistriesConfig actualConfig = RegistriesConfigLocator.initFromEnvironmentOrNull(env);

        final RegistriesConfig expectedConfig = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setExtra(Constants.OFFERING, "acme-magic")
                        .build())
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.other.io")
                        .setExtra(Constants.OFFERING, "other-cloud")
                        .build())
                .build();

        assertThat(actualConfig).isEqualTo(expectedConfig);

        final RegistriesConfig completeConfig = serializeDeserialize(actualConfig);
        assertThat(completeConfig).isEqualTo(expectedConfig);
    }

    @Test
    void testRecommendStreamsFrom() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRIES, "registry.acme.org,registry.other.io");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_ACME_ORG_"
                + RegistriesConfigLocator.RECOMMEND_STREAMS_FROM_ + "ORG_ACME_PLATFORM", "1.1");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_OTHER_IO_"
                + RegistriesConfigLocator.RECOMMEND_STREAMS_FROM_ + "IO_OTHER_PLATFORM", "2.2");
        env.put(RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX + "REGISTRY_OTHER_IO_"
                + RegistriesConfigLocator.RECOMMEND_STREAMS_FROM_ + "IO_ANOTHER_PLATFORM", "3.3");

        final RegistriesConfig actualConfig = RegistriesConfigLocator.initFromEnvironmentOrNull(env);

        final RegistriesConfig expectedConfig = RegistriesConfig.builder()
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.acme.org")
                        .setExtra(Constants.RECOMMEND_STREAMS_FROM, Map.of("org.acme.platform", "1.1"))
                        .build())
                .setRegistry(RegistryConfig.builder()
                        .setId("registry.other.io")
                        .setExtra(Constants.RECOMMEND_STREAMS_FROM, Map.of(
                                "io.other.platform", "2.2",
                                "io.another.platform", "3.3"))
                        .build())
                .build();

        assertThat(actualConfig).isEqualTo(expectedConfig);

        final RegistriesConfig completeConfig = serializeDeserialize(actualConfig);
        assertThat(completeConfig).isEqualTo(expectedConfig);
    }

    private static RegistriesConfig serializeDeserialize(RegistriesConfig config) throws Exception {
        final StringWriter buf = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(config, buf);
        try (StringReader reader = new StringReader(buf.toString())) {
            return RegistriesConfigLocator.load(reader);
        }
    }
}

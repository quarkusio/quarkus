package io.quarkus.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.registry.builder.RegistryBuilder;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.model.Registry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultExtensionRegistryTest {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    static DefaultExtensionRegistry extensionRegistry;

    @BeforeAll
    static void setUp() throws IOException {
        Repository repository = Repository.parse(Paths.get("src/test/resources/registry/repository"), OBJECT_MAPPER);
        RegistryBuilder registryBuilder = new RegistryBuilder();
        RepositoryIndexer indexer = new RepositoryIndexer(new TestArtifactResolver());
        indexer.index(repository, registryBuilder);
        Registry registry = registryBuilder.build();
        extensionRegistry = new DefaultExtensionRegistry(registry);
    }

    @Test
    void serializationShouldKeepValues(@TempDir Path tmpDir) throws IOException {
        File tmpFile = tmpDir.resolve("registry.json").toFile();
        OBJECT_MAPPER.writeValue(tmpFile, extensionRegistry.getRegistry());
        Registry newRegistry = OBJECT_MAPPER.readValue(tmpFile, Registry.class);
        assertThat(newRegistry).isEqualToComparingFieldByField(extensionRegistry.getRegistry());
    }

    @Test
    void shouldReturnFourQuarkusCoreVersions() {
        assertThat(extensionRegistry.getQuarkusCoreVersions()).containsExactly(
                "1.6.0.Final",
                "1.5.2.Final",
                "1.3.2.Final");
    }

    @Test
    void shouldLookupPlatformForDependentExtensionInQuarkusFinal() {
        ExtensionInstallPlan result = extensionRegistry.planInstallation("1.3.2.Final",
                Arrays.asList("quarkus-arc", "quarkus-vertx"));
        assertThat(result).isNotNull();
        assertThat(result.getPlatforms()).hasSize(2);
        assertThat(result.getPlatforms())
                .extracting(AppArtifactCoords::getArtifactId)
                .contains("quarkus-universe-bom", "quarkus-bom");
        assertThat(result.getManagedExtensions()).hasSize(2);
        assertThat(result.getIndependentExtensions()).isEmpty();
    }

    @Test
    void shouldLookupNoPlatformForIndependentExtension() {
        ExtensionInstallPlan result = extensionRegistry.planInstallation("1.3.1.Final",
                Collections.singletonList("myfaces-quarkus-runtime"));
        assertThat(result).isNotNull();
        assertThat(result.getPlatforms()).isEmpty();
        assertThat(result.getManagedExtensions()).isEmpty();
        assertThat(result.getIndependentExtensions()).hasSize(1);
        assertThat(result.getIndependentExtensions().iterator().next())
                .hasFieldOrPropertyWithValue("groupId", "org.apache.myfaces.core.extensions.quarkus")
                .hasFieldOrPropertyWithValue("artifactId", "myfaces-quarkus-runtime")
                .hasFieldOrPropertyWithValue("version", "2.3-next-M2");
    }
}

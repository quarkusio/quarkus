package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithSemiAutoPostgresBindingTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-semi-auto-postgres-binding")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-semi-auto-postgres-binding.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-datasource", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-kubernetes-service-binding", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));

        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("kubernetes-with-semi-auto-postgres-binding");
                });
                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "ServiceBinding".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(GenericKubernetesResource.class, sb -> assertThat(sb)
                        .hasFieldOrPropertyWithValue("metadata.name", "kubernetes-with-semi-auto-postgres-binding-postgresql")
                        .returns("apps", s -> s.get("spec", "application", "group"))
                        .returns("v1", s -> s.get("spec", "application", "version"))
                        .returns("Deployment", s -> s.get("spec", "application", "kind"))
                        .extracting(s -> s.get("spec", "services"))
                        .asList()
                        .singleElement().asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsEntry("group", "my.custom-operator.com")
                        .containsEntry("version", "v1alpha1")
                        .containsEntry("kind", "Postgres")
                        .containsEntry("name", "postgresql"));
    }
}

package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dekorate.servicebinding.model.ServiceBinding;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithAutoPostgresBindingTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-auto-postgres-binding")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-auto-postgres-binding.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(
                    Arrays.asList(
                            new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-datasource", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-kubernetes-service-binding", Version.getVersion())));

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
                    assertThat(m.getName()).isEqualTo("kubernetes-with-auto-postgres-binding");
                });
                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "ServiceBinding".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(ServiceBinding.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("kubernetes-with-auto-postgres-binding-postgresql");
                });
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getApplication()).satisfies(a -> {
                        assertThat(a.getGroup()).isEqualTo("apps");
                        assertThat(a.getVersion()).isEqualTo("v1");
                        assertThat(a.getKind()).isEqualTo("Deployment");
                    });

                    assertThat(spec.getServices()).hasOnlyOneElementSatisfying(service -> {
                        assertThat(service.getGroup()).isEqualTo("postgres-operator.crunchydata.com");
                        assertThat(service.getVersion()).isEqualTo("v1beta1");
                        assertThat(service.getKind()).isEqualTo("PostgresCluster");
                        assertThat(service.getName()).isEqualTo("postgresql");
                    });
                });
            });
        });
    }
}

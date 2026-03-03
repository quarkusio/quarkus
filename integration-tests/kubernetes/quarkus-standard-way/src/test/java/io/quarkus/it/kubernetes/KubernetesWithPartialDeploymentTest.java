
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Integration test for issue #50791.
 */
public class KubernetesWithPartialDeploymentTest {

    static final String APP_NAME = "kubernetes-with-partial-deployment-metadata-only";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-partial-deployment.properties")
            .setLogFileName("k8s.log")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "kubernetes.yml"),
                    "manifests/kubernetes-with-partial-deployment-metadata-only/kubernetes.yml")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())))
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(
                            KubernetesWithCustomResourcesTest.CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);

                    assertThat(m.getAnnotations())
                            .containsEntry("custom-annotation", "custom-value")
                            .containsEntry("app.kubernetes.io/managed-by", "user");

                    assertThat(m.getLabels())
                            .containsEntry("custom-label", "from-user-manifest")
                            .containsEntry("app-tier", "backend")
                            .containsEntry("app.kubernetes.io/name", APP_NAME)
                            .containsEntry("app.kubernetes.io/version", "0.1-SNAPSHOT");
                });

                assertThat(d.getSpec()).isNotNull().satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getReplicas()).isEqualTo(1);

                    assertThat(deploymentSpec.getSelector()).isNotNull().satisfies(ls -> {
                        assertThat(ls.getMatchLabels())
                                .isNotEmpty()
                                .containsEntry("app.kubernetes.io/name", APP_NAME);
                    });

                    assertThat(deploymentSpec.getTemplate()).isNotNull().satisfies(t -> {
                        assertThat(t.getMetadata()).isNotNull().satisfies(m -> {
                            assertThat(m.getLabels())
                                    .isNotEmpty()
                                    .containsEntry("app.kubernetes.io/name", APP_NAME);
                        });

                        assertThat(t.getSpec()).isNotNull().satisfies(podSpec -> {
                            assertThat(podSpec.getTerminationGracePeriodSeconds()).isEqualTo(10);
                            assertThat(podSpec.getContainers())
                                    .isNotEmpty()
                                    .allMatch(c -> APP_NAME.equals(c.getName()));
                        });
                    });
                });
            });
        });
    }

    @Test
    public void assertDeploymentSpecIsComplete() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = kubernetesList.stream()
                .filter(i -> i instanceof Deployment)
                .map(i -> (Deployment) i)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment not found"));

        assertThat(deployment.getSpec())
                .as("Deployment spec should not be null (bug from issue #50791)")
                .isNotNull();

        assertThat(deployment.getSpec().getReplicas())
                .as("Deployment spec should have replicas set")
                .isNotNull()
                .isPositive();

        assertThat(deployment.getSpec().getTemplate())
                .as("Deployment spec should have pod template")
                .isNotNull();

        assertThat(deployment.getSpec().getSelector())
                .as("Deployment spec should have selector")
                .isNotNull();
    }
}

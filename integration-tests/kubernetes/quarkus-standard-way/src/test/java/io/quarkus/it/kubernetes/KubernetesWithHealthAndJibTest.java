package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithHealthAndJibTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("health-and-jib")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .withConfigurationResource("kubernetes-with-health-and-jib.properties")
            .setForcedDependencies(
                    Arrays.asList(
                            new AppArtifact("io.quarkus", "quarkus-smallrye-health", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-container-image-jib", Version.getVersion())));

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
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("with-health-and-jib");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getReadinessProbe()).isNotNull().satisfies(p -> {
                                assertProbePath(p, "/q/health/ready");
                            });
                            assertThat(container.getLivenessProbe()).isNotNull().satisfies(p -> {
                                assertProbePath(p, "/q/health/live");
                            });
                            assertThat(container.getImagePullPolicy()).isEqualTo("Always");
                        });
                    });
                });
            });
        });
    }

    private void assertProbePath(Probe p, String expectedPath) {
        assertThat(p.getHttpGet()).satisfies(h -> {
            assertThat(h.getPath()).isEqualTo(expectedPath);
        });
    }
}

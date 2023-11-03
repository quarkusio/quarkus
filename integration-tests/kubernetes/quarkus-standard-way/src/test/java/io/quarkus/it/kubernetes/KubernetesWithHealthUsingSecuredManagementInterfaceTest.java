package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithHealthUsingSecuredManagementInterfaceTest {

    private static final String NAME = "kubernetes-with-health-and-secured-management";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .addCustomResourceEntry(Path.of("src", "main", "resources", "META-INF", "server.keystore"),
                    "manifests/" + NAME + "/server.keystore")
            .setLogFileName("k8s.log")
            .withConfigurationResource(NAME + ".properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion())))
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(
                            OpenshiftWithCommonResourcesTest.CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

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
                assertThat(m.getName()).isEqualTo(NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getReadinessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getInitialDelaySeconds()).isEqualTo(5);
                                assertProbePath(p, "/q/health/ready");

                                assertNotNull(p.getHttpGet());
                                assertEquals("HTTPS", p.getHttpGet().getScheme());
                                assertEquals(9000, p.getHttpGet().getPort().getIntVal());
                            });
                            assertThat(container.getLivenessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getInitialDelaySeconds()).isEqualTo(20);
                                assertProbePath(p, "/liveness");

                                assertNotNull(p.getHttpGet());
                                assertEquals("HTTPS", p.getHttpGet().getScheme());
                                assertEquals(9000, p.getHttpGet().getPort().getIntVal());
                            });
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

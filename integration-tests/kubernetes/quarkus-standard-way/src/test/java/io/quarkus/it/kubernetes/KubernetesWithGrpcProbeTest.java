package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithGrpcProbeTest {

    private static final String APP_NAME = "kubernetes-with-grpc-probe";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.readiness-probe.grpc-action", "9191:my-service")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion())));

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
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement()
                                .satisfies(container -> {
                                    assertThat(container.getReadinessProbe()).isNotNull().satisfies(p -> {
                                        assertEquals(p.getGrpc().getPort().intValue(), 9191);
                                        assertEquals(p.getGrpc().getService(), "my-service");
                                    });
                                });
                    });
                });
            });
        });
    }
}

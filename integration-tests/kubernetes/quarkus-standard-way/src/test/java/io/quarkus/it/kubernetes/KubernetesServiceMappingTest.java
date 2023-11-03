
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesServiceMappingTest {

    private static final String APP_NAME = "kubernetes-service-mapping";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

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
                assertThat(m.getName()).isEqualTo("kubernetes-service-mapping");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        List<ContainerPort> ports = podSpec.getContainers().get(0).getPorts();
                        assertThat(ports.size()).isEqualTo(2);
                        assertTrue(ports.stream().anyMatch(port -> "http".equals(port.getName())
                                && port.getContainerPort() == 8080
                                && "TCP".equals(port.getProtocol())),
                                "http port not found in the pod containers!");
                        assertTrue(ports.stream().anyMatch(port -> "debug".equals(port.getName())
                                && port.getContainerPort() == 5005
                                && "UDP".equals(port.getProtocol())),
                                "debug port not found in the pod containers!");
                    });
                });
            });
        });

        assertThat(kubernetesList.get(1)).isInstanceOfSatisfying(Service.class, s -> {
            assertThat(s.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("kubernetes-service-mapping");
            });
            assertThat(s.getSpec()).satisfies(serviceSpec -> {
                assertThat(serviceSpec.getPorts().size()).isEqualTo(2);
                assertTrue(serviceSpec.getPorts().stream().anyMatch(port -> "http".equals(port.getName())
                        && port.getTargetPort().getIntVal() == 8080
                        && "TCP".equals(port.getProtocol())
                        && port.getPort() == 8080),
                        () -> "http port not found in the service!");
                assertTrue(serviceSpec.getPorts().stream().anyMatch(port -> "debug".equals(port.getName())
                        && port.getTargetPort().getIntVal() == 5005
                        && "UDP".equals(port.getProtocol())
                        && port.getPort() == 5005),
                        () -> "debug port not found in the service!");
            });
        });

    }
}

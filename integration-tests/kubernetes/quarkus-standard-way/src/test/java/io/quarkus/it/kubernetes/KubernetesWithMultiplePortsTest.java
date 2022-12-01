package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithMultiplePortsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-multiple-ports")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-multiple-ports.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).hasSize(2);

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("kubernetes-with-multiple-ports");
                });

                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                            assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                                assertThat(container.getPorts()).hasSize(2);
                                assertThat(container.getPorts()).filteredOn(cp -> cp.getContainerPort() == 8080).singleElement()
                                        .satisfies(c -> assertThat(c.getName()).isEqualTo("http"));
                                assertThat(container.getPorts()).filteredOn(cp -> cp.getContainerPort() == 5005).singleElement()
                                        .satisfies(c -> assertThat(c.getName()).isEqualTo("remote"));
                            });
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertEquals("NodePort", spec.getType());
                    assertThat(spec.getPorts()).hasSize(2);
                    assertThat(spec.getPorts()).filteredOn(sp -> sp.getPort() == 8080).singleElement().satisfies(p -> {
                        assertThat(p.getNodePort()).isEqualTo(30000);
                    });
                    assertThat(spec.getPorts()).filteredOn(sp -> sp.getPort() == 5005).singleElement().satisfies(p -> {
                        assertThat(p.getNodePort()).isEqualTo(31000);
                    });
                });
            });
        });
    }

}

package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class MinikubeWithDefaultsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("minikube-with-defaults")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("minikube-with-defaults.properties")
            .setForcedDependencies(
                    Collections.singletonList(new AppArtifact("io.quarkus", "quarkus-minikube", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("minikube.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("minikube.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("minikube.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {

                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isNull();
                });

                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getReplicas()).isEqualTo(1);
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                            assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                                assertThat(container.getImagePullPolicy()).isEqualTo("IfNotPresent");
                            });
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isNull();
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertEquals("NodePort", spec.getType());
                    assertThat(spec.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                        assertThat(p.getNodePort()).isNotNull();
                    });
                });
            });
        });
    }
}

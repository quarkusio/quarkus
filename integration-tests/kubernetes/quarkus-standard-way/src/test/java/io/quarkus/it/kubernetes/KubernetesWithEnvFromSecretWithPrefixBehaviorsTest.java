package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithEnvFromSecretWithPrefixBehaviorsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("env-from-secret-with-prefix-behaviors")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-env-from-secret-with-prefix-behaviors.properties");

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
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("env-from-secret-with-prefix-behaviors");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {

                            assertThat(container.getEnvFrom())
                                    .anyMatch(item -> item.getPrefix() != null && !item.getPrefix().isBlank()
                                            && item.getSecretRef().getName().equals("another"));
                            assertThat(container.getEnvFrom())
                                    .anyMatch(item -> item.getPrefix() != null && !item.getPrefix().isBlank()
                                            && item.getSecretRef().getName().equals("secrets"));
                            assertThat(container.getEnvFrom()).anyMatch(
                                    item -> item.getPrefix() == null && item.getSecretRef().getName().equals("without"));
                        });
                    });
                });
            });
        });
    }
}

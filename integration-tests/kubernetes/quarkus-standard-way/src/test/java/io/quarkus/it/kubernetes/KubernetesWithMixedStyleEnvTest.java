package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithMixedStyleEnvTest {
    private static final String APPLICATION_NAME = "mixed-style";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-" + APPLICATION_NAME + "-env.properties");

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
                assertThat(m.getName()).isEqualTo(APPLICATION_NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getEnv())
                                    .filteredOn(env -> "FROMFIELD".equals(env.getName()))
                                    .singleElement().satisfies(
                                            env -> assertThat(env.getValueFrom().getFieldRef().getFieldPath())
                                                    .isEqualTo("metadata.name"));
                            assertThat(container.getEnv())
                                    .filteredOn(env -> "ENVVAR".equals(env.getName()))
                                    .singleElement().satisfies(env -> assertThat(env.getValue()).isEqualTo("value"));
                            final List<EnvFromSource> envFrom = container.getEnvFrom();
                            assertThat(envFrom).hasSize(2);
                            assertThat(envFrom)
                                    .filteredOn(e -> e.getSecretRef() != null)
                                    .singleElement().satisfies(
                                            e -> assertThat(e.getSecretRef().getName()).isEqualTo("secretName"));
                            assertThat(envFrom)
                                    .filteredOn(e -> e.getConfigMapRef() != null)
                                    .singleElement().satisfies(
                                            e -> assertThat(e.getConfigMapRef().getName()).isEqualTo("configName"));
                        });
                    });
                });
            });
        });
    }
}

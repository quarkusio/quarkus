
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

public class KubernetesWithEnvFromConfigMapTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("env-from-configmap")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-env-from-configmap.properties");

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
                assertThat(m.getName()).isEqualTo("env-from-configmap");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {

                            assertThat(container.getEnvFrom()).filteredOn(env -> env.getConfigMapRef() != null
                                    && env.getConfigMapRef().getName() != null
                                    && env.getConfigMapRef().getName().equals("my-configmap"))
                                    .singleElement().satisfies(env -> {
                                        assertThat(env.getConfigMapRef()).satisfies(configmapRef -> {
                                            assertThat(configmapRef.getOptional()).isNull();
                                        });
                                    });

                            assertThat(container.getEnv()).filteredOn(env -> "DB_DATABASE".equals(env.getName()))
                                    .singleElement().satisfies(env -> {
                                        assertThat(env.getValueFrom()).satisfies(valueFrom -> {
                                            assertThat(valueFrom.getConfigMapKeyRef()).satisfies(configMapKeyRef -> {
                                                assertThat(configMapKeyRef.getKey()).isEqualTo("database.name");
                                                assertThat(configMapKeyRef.getName()).isEqualTo("db-config");
                                                assertThat(configMapKeyRef.getOptional()).isNull();
                                            });
                                        });
                                    });
                        });
                    });
                });
            });
        });
    }
}

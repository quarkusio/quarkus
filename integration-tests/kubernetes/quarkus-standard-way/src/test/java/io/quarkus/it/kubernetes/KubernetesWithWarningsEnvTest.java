package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithWarningsEnvTest {
    private static final String APPLICATION_NAME = "warnings";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogRecordPredicate(r -> "io.quarkus.kubernetes.deployment.EnvVarValidator".equals(r.getLoggerName())
                    || "io.quarkus.kubernetes.spi.KubernetesEnvBuildItem".equals(r.getLoggerName()))
            .withConfigurationResource("kubernetes-with-" + APPLICATION_NAME + "-env.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void ensureProperQuarkusPropertiesLogged() throws IOException {
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
                                    .filteredOn(env -> "MY_FIELD".equals(env.getName()))
                                    .singleElement().satisfies(
                                            env -> assertThat(env.getValueFrom().getFieldRef().getFieldPath())
                                                    .isEqualTo("newField"));
                            assertThat(container.getEnv())
                                    .filteredOn(env -> "MY_VAR".equals(env.getName()))
                                    .singleElement().satisfies(env -> assertThat(env.getValue()).isEqualTo("newVariable"));
                            final List<EnvFromSource> envFrom = container.getEnvFrom();
                            assertThat(envFrom).hasSize(2);
                            assertThat(envFrom)
                                    .filteredOn(e -> e.getSecretRef() != null)
                                    .singleElement().satisfies(
                                            e -> assertThat(e.getSecretRef().getName()).isEqualTo("secret"));
                            assertThat(envFrom)
                                    .filteredOn(e -> e.getConfigMapRef() != null)
                                    .singleElement().satisfies(
                                            e -> assertThat(e.getConfigMapRef().getName()).isEqualTo("configMap"));
                        });
                    });
                });
            });
        });

        List<LogRecord> buildLogRecords = prodModeTestResults.getRetainedBuildLogRecords();
        buildLogRecords.forEach(l -> System.out.println("l = " + l.getMessage()));
        assertThat(buildLogRecords).hasSize(4);
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("my-field"))
                .singleElement().satisfies(r -> assertThat(r.getMessage()).contains("newField"));
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("my-var"))
                .singleElement().satisfies(r -> assertThat(r.getMessage()).contains("newVariable"));
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("configMap"))
                .singleElement().satisfies(
                        r -> assertThat(r.getMessage().contains("'xxx'") && r.getMessage().contains("'secret'")));
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("secret"))
                .hasSize(2);
    }
}

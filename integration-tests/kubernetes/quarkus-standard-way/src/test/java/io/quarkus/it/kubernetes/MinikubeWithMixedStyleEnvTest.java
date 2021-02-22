package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class MinikubeWithMixedStyleEnvTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("minikube-with-mixed-style-env")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("minikube-with-mixed-style-env.properties")
            .setForcedDependencies(
                    Collections.singletonList(new AppArtifact("io.quarkus", "quarkus-minikube", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("minikube.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("minikube.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("minikube.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
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
        });
    }
}

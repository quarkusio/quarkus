package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithSidecarAndJibTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("sidecar-test")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-sidecar-and-jib.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(
                    Collections.singletonList(
                            new AppArtifact("io.quarkus", "quarkus-container-image-jib", Version.getVersion())));

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
                assertThat(m.getName()).isEqualTo("sidecar-test");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).hasSize(2);
                        assertSidecar(podSpec);
                        assertApplicationContainer(podSpec);
                    });
                });
            });
        });
    }

    private void assertApplicationContainer(io.fabric8.kubernetes.api.model.PodSpec podSpec) {
        assertThat(podSpec.getContainers()).filteredOn(ps -> "sidecar-test".equals(ps.getName()))
                .singleElement().satisfies(c -> {
                    assertThat(c.getImage()).isEqualTo("docker.io/somegrp/sidecar-test:0.1-SNAPSHOT");
                    assertThat(c.getImagePullPolicy()).isEqualTo("Always");
                    assertThat(c.getCommand()).isEmpty();
                    assertThat(c.getArgs()).isEmpty();
                    assertThat(c.getWorkingDir()).isNull();
                    assertThat(c.getVolumeMounts()).isEmpty();
                    assertThat(c.getPorts()).singleElement().satisfies(p -> assertThat(p.getContainerPort()).isEqualTo(8080));
                    assertThat(c.getEnv()).allSatisfy(e -> assertThat(e.getName()).isNotEqualToIgnoringCase("foo"));
                });
    }

    private void assertSidecar(io.fabric8.kubernetes.api.model.PodSpec podSpec) {
        assertThat(podSpec.getContainers()).filteredOn(ps -> "sc".equals(ps.getName()))
                .singleElement().satisfies(c -> {
                    assertThat(c.getImage()).isEqualTo("quay.io/sidecar/image:2.1");
                    assertThat(c.getImagePullPolicy()).isEqualTo("IfNotPresent");
                    assertThat(c.getCommand()).containsOnly("ls");
                    assertThat(c.getArgs()).containsOnly("-l");
                    assertThat(c.getWorkingDir()).isEqualTo("/work");
                    assertThat(c.getVolumeMounts()).singleElement().satisfies(volumeMount -> {
                        assertThat(volumeMount.getName()).isEqualTo("app-config");
                        assertThat(volumeMount.getMountPath()).isEqualTo("/deployments/config");
                    });
                    assertThat(c.getPorts()).singleElement().satisfies(p -> {
                        assertThat(p.getContainerPort()).isEqualTo(3000);
                    });
                    assertThat(c.getEnv()).singleElement().satisfies(e -> {
                        assertThat(e.getName()).isEqualTo("FOO");
                        assertThat(e.getValue()).isEqualTo("bar");
                    });
                });
    }
}

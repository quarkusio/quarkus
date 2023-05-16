package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithFlywayInitTest {

    private static final String NAME = "kubernetes-with-flyway";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            .setForcedDependencies(Arrays.asList(
                    new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    new AppArtifact("io.quarkus", "quarkus-flyway", Version.getVersion())));

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

        Optional<Deployment> deployment = kubernetesList.stream()
                .filter(d -> "Deployment".equals(d.getKind())
                        && NAME.equals(d.getMetadata().getName()))
                .map(d -> (Deployment) d).findAny();

        assertTrue(deployment.isPresent());
        assertThat(deployment).satisfies(j -> j.isPresent());
        assertThat(deployment.get()).satisfies(d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getInitContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getName()).isEqualTo("init");
                            assertThat(container.getImage()).isEqualTo("groundnuty/k8s-wait-for:no-root-v1.7");
                        });

                    });
                });
            });
        });

        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()) && (NAME + "-flyway-init").equals(j.getMetadata().getName()))
                .map(j -> (Job) j)
                .findAny();
        assertTrue(job.isPresent());

        assertThat(job.get()).satisfies(j -> {
            assertThat(j.getSpec()).satisfies(jobSpec -> {
                assertThat(jobSpec.getCompletionMode()).isEqualTo("NonIndexed");
                assertThat(jobSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getRestartPolicy()).isEqualTo("OnFailure");
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getName()).isEqualTo(NAME + "-flyway-init");
                            assertThat(container.getEnv()).filteredOn(env -> "QUARKUS_FLYWAY_ENABLED".equals(env.getName()))
                                    .singleElement().satisfies(env -> {
                                        assertThat(env.getValue()).isEqualTo("true");
                                    });
                            assertThat(container.getEnv())
                                    .filteredOn(env -> "QUARKUS_INIT_AND_EXIT".equals(env.getName())).singleElement()
                                    .satisfies(env -> {
                                        assertThat(env.getValue()).isEqualTo("true");
                                    });
                        });
                    });
                });
            });
        });

        Optional<RoleBinding> roleBinding = kubernetesList.stream().filter(
                r -> r instanceof RoleBinding && (NAME + "-view-jobs").equals(r.getMetadata().getName()))
                .map(r -> (RoleBinding) r).findFirst();
        assertTrue(roleBinding.isPresent());
    }
}

package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithFlywayInitWithJobDisabledTest {

    private static final String NAME = "kubernetes-with-flyway-with-job-disabled";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.init-tasks.\"flyway\".enabled", "false")
            .setForcedDependencies(Arrays.asList(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion())));

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
                        assertThat(podSpec.getInitContainers()).noneSatisfy(container -> {
                            assertThat(container.getName()).startsWith("wait-for");
                        });
                    });
                });
            });
        });

        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()))
                .map(j -> (Job) j)
                .findAny();
        assertFalse(job.isPresent());

        Optional<RoleBinding> roleBinding = kubernetesList.stream().filter(
                r -> r instanceof RoleBinding && (NAME + "-view-jobs").equals(r.getMetadata().getName()))
                .map(r -> (RoleBinding) r).findFirst();
        assertFalse(roleBinding.isPresent());
    }
}

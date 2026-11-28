package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithFlywayInitAndTtlTest {

    private static final String NAME = "kubernetes-with-flyway-ttl";
    private static final String TASK_NAME = "flyway";
    private static final int TTL = 30;

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            .overrideConfigKey("quarkus.kubernetes.init-task-defaults.ttl-seconds-after-finished", String.valueOf(TTL))
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

        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        String jobName = NAME + "-" + TASK_NAME + "-init";
        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()) && jobName.equals(j.getMetadata().getName()))
                .map(j -> (Job) j)
                .findAny();

        assertThat(job).isPresent().get().satisfies(j -> {
            assertThat(j.getSpec()).satisfies(jobSpec -> {
                assertThat(jobSpec.getTtlSecondsAfterFinished()).isEqualTo(TTL);
            });
        });
    }
}

package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.builder.BuildContext;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that an {@link InitTaskBuildItem} carrying a {@code ttlSecondsAfterFinished} default is honoured when no
 * configuration overrides it.
 */
public class KubernetesWithInitTaskBuildItemTtlTest {

    private static final String NAME = "kubernetes-with-init-task-build-item-ttl";
    private static final int TTL = 45;

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(InitTaskProducer.class,
                            Collections.singletonList(InitTaskBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));

        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()))
                .map(j -> (Job) j)
                .findAny();

        assertThat(job).isPresent().get().satisfies(j -> assertThat(j.getSpec())
                .satisfies(jobSpec -> assertThat(jobSpec.getTtlSecondsAfterFinished()).isEqualTo(TTL)));
    }

    public static class InitTaskProducer extends ProdModeTestBuildStep {

        public InitTaskProducer(Map<String, Object> testContext) {
            super(testContext);
        }

        @Override
        public void execute(BuildContext context) {
            context.produce(InitTaskBuildItem.create()
                    .withName("init")
                    .withCommand(List.of("/bin/sh"))
                    .withTtlSecondsAfterFinished(TTL));
        }
    }
}

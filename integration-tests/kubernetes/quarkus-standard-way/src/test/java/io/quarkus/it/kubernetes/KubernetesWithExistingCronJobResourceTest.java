
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithExistingCronJobResourceTest {

    static final String APP_NAME = "kubernetes-with-existing-cronjob-resource";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "kubernetes.yml"),
                    "manifests/" + APP_NAME + "/kubernetes.yml")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())))
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(
                            KubernetesWithCustomResourcesTest.CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

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

        assertThat(kubernetesList).filteredOn(i -> i instanceof CronJob).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(CronJob.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });

                assertThat(s.getSpec().getSchedule()).isEqualTo("0 0 0 0 *");

                assertThat(s.getSpec().getJobTemplate().getSpec()).satisfies(jobSpec -> {
                    assertThat(jobSpec.getParallelism()).isEqualTo(10);
                    assertThat(jobSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(templateSpec -> {
                            assertThat(templateSpec.getRestartPolicy()).isEqualTo("Never");
                            assertThat(templateSpec.getContainers()).allMatch(c -> {
                                return APP_NAME.equals(c.getName())
                                        && c.getArgs().size() == 2
                                        && c.getArgs().get(0).equals("A")
                                        && c.getArgs().get(1).equals("B");
                            });
                        });
                    });
                    assertThat(jobSpec.getSelector()).isEqualTo(null);
                });
            });
        });
    }
}

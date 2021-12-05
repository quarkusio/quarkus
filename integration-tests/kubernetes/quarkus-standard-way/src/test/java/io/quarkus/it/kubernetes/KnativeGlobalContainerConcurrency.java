package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeGlobalContainerConcurrency {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-global-container-concurrency")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-global-container-concurrency.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "ConfigMap".equals(i.getKind())).singleElement().satisfies(c -> {
            assertThat(c.getMetadata()).satisfies(m -> assertThat(m.getName()).isEqualTo("config-autoscaler"));
            assertThat(c).isInstanceOfSatisfying(ConfigMap.class, m -> {
                assertThat(m.getData()).contains(entry("container-concurrency", "100"));
            });
        });
    }
}

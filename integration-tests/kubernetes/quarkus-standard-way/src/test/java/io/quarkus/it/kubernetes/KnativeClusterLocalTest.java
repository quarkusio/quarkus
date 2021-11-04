
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeClusterLocalTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-cluster-local").setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-cluster-local.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));

        assertThat(getKNativeService(kubernetesDir)).satisfies(service -> {
            assertThat(service.getMetadata()).satisfies(m -> {
                assertThat(m.getLabels()).contains(entry("serving.knative.dev/visibility", "cluster-local"));
            });
        });
    }

    private Service getKNativeService(Path kubernetesDir) throws IOException {
        String[] yamlFiles = DeserializationUtil.splitDocument(
                Files.readAllLines(kubernetesDir.resolve("knative.yml"), StandardCharsets.UTF_8).toArray(new String[0]));

        for (String yamlFile : yamlFiles) {
            if (yamlFile.contains("Service") && !yamlFile.contains("ServiceAccount")) {
                return DeserializationUtil.MAPPER.readValue(yamlFile, Service.class);
            }
        }

        fail("No KNative Service was generated");
        return null; // keep the compiler happy
    }
}

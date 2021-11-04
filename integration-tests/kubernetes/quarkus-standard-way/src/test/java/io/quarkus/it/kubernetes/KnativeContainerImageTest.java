
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
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

public class KnativeContainerImageTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-with-container-image").setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-container-image.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));

        assertThat(getKNativeService(kubernetesDir).getSpec()).satisfies(serviceSpec -> {
            assertThat(serviceSpec.getTemplate()).satisfies(revisionTemplate -> {
                assertThat(revisionTemplate.getSpec()).satisfies(spec -> {
                    assertThat(spec.getContainers()).satisfies(containers -> {
                        assertThat(containers.get(0)).satisfies(c -> assertThat(c.getImage())
                                .isEqualTo("quay.io/grp/knative-with-container-image:0.1-SNAPSHOT"));
                    });
                });
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

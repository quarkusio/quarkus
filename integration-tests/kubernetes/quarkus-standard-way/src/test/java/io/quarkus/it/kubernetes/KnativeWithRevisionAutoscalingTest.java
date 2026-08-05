package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithRevisionAutoscalingTest {

    private static final String NAME = "knative-with-revision-autoscaling";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-revision-autoscaling.properties");

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

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            Service service = (Service) i;
            assertThat(service.getMetadata().getName()).isEqualTo(NAME);
            final var template = service.getSpec().getTemplate();
            assertThat(template.getSpec().getContainerConcurrency()).isEqualTo(5);

            Map<String, String> annotations = template.getMetadata().getAnnotations();
            // remove Quarkus-specific annotations to check ordering of knative annotations
            final var withoutQuarkusAnnotations = annotations.entrySet().stream()
                    .filter(e -> !e.getKey().startsWith("app.quarkus.io"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<String, String> expected = new TreeMap<>(Map.of(
                    "autoscaling.knative.dev/class", "kpa.autoscaling.knative.dev",
                    "autoscaling.knative.dev/metric", "cpu",
                    "autoscaling.knative.dev/target-utilization-percentage", "55",
                    "autoscaling.knative.dev/target", "80"));
            assertThat(withoutQuarkusAnnotations).isEqualTo(expected);
        });
    }
}

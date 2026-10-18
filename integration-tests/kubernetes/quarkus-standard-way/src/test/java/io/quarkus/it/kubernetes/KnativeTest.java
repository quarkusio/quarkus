package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeTest {
    private static final String VERSION = "0.1-SNAPSHOT";
    public static final String APPLICATION_NAME = "knative";
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion(VERSION)
            .withConfigurationResource("knative.properties");

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
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(s.getMetadata()).satisfies(m -> {
                        assertThat(m.getNamespace()).isNull();
                        assertThat(m.getAnnotations().get("app.quarkus.io/quarkus-version")).isNotBlank();
                        final var labels = m.getLabels();
                        final var expected = new TreeMap(Map.of("app.kubernetes.io/managed-by", "quarkus",
                                "app.kubernetes.io/name", APPLICATION_NAME,
                                "app.kubernetes.io/version", VERSION));
                        assertThat(labels).isEqualTo(expected);

                    });

                    assertThat(spec.getTemplate()).satisfies(template -> {
                        assertThat(template.getSpec()).satisfies(templateSpec -> {
                            assertThat(templateSpec.getContainers()).hasSize(1).singleElement().satisfies(c -> {
                                assertThat(c.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                                    assertThat(p.getName()).isEqualTo("http1");
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}

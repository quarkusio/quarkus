package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithApplicationPropertiesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("knative-with-application-properties")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-application.properties");

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
                        assertThat(m.getName()).isEqualTo("test-it");
                        assertThat(m.getLabels()).contains(entry("foo", "bar"));
                        assertThat(m.getAnnotations()).contains(entry("bar", "baz"));
                        assertThat(m.getNamespace()).isEqualTo("applications");
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

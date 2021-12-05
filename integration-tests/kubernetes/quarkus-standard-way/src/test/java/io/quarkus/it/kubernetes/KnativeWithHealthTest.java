package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Probe;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithHealthTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-health")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-health.properties")
            .setForcedDependencies(
                    Collections.singletonList(
                            new AppArtifact("io.quarkus", "quarkus-smallrye-health", Version.getVersion())));

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
                    });

                    assertThat(spec.getTemplate()).satisfies(template -> {
                        assertThat(template.getSpec()).satisfies(templateSpec -> {
                            assertThat(templateSpec.getContainers()).hasSize(1).singleElement().satisfies(c -> {
                                assertThat(c.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                                    assertThat(p.getName()).isEqualTo("http1");
                                });
                                assertThat(c.getReadinessProbe()).isNotNull().satisfies(p -> {
                                    assertThat(p.getInitialDelaySeconds()).isEqualTo(0);
                                    assertProbePath(p, "/q/health/ready");

                                    assertNotNull(p.getHttpGet());
                                    assertNull(p.getHttpGet().getPort());
                                });
                                assertThat(c.getLivenessProbe()).isNotNull().satisfies(p -> {
                                    assertThat(p.getInitialDelaySeconds()).isEqualTo(20);
                                    assertProbePath(p, "/q/health/live");

                                    assertNotNull(p.getHttpGet());
                                    assertNull(p.getHttpGet().getPort());
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    private void assertProbePath(Probe p, String expectedPath) {
        assertThat(p.getHttpGet()).satisfies(h -> {
            assertThat(h.getPath()).isEqualTo(expectedPath);
        });
    }
}

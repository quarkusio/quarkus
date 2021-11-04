package io.quarkus.it.kubernetes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithMetricsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .withConfigurationResource("kubernetes-with-metrics.properties")
            .setForcedDependencies(
                    List.of(
                            new AppArtifact("io.quarkus", "quarkus-smallrye-metrics", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-kubernetes-client", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

    @Test
    public void assertApplicationRuns() {
        assertThat(logfile).isRegularFile().hasFileName("k8s.log");
        TestUtil.assertLogFileContents(logfile, "kubernetes", "metrics");

        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(h -> "Deployment".equals(h.getKind())).singleElement()
                .isInstanceOfSatisfying(Deployment.class, d -> {
                    assertThat(d.getMetadata()).satisfies(m -> assertThat(m.getName()).isEqualTo("metrics"));

                    assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                        assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                            assertThat(t.getMetadata()).satisfies(meta -> {
                                assertThat(meta.getAnnotations()).contains(entry("prometheus.io/scrape", "true"),
                                        entry("prometheus.io/path", "/q/metrics"), entry("prometheus.io/port", "9090"),
                                        entry("prometheus.io/scheme", "http"));
                            });
                        });
                    });
                });

        assertThat(kubernetesList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata().getAnnotations()).contains(entry("prometheus.io/scrape", "true"),
                    entry("prometheus.io/path", "/q/metrics"), entry("prometheus.io/port", "9090"),
                    entry("prometheus.io/scheme", "http"));
        });

        assertThat(kubernetesList).filteredOn(h -> "ServiceAccount".equals(h.getKind())).singleElement().satisfies(h -> {
            if (h.getMetadata().getAnnotations() != null) {
                assertThat(h.getMetadata().getAnnotations()).doesNotContainKeys("prometheus.io/scrape", "prometheus.io/path",
                        "prometheus.io/port", "prometheus.io/scheme");
            }
        });

        assertThat(kubernetesList).filteredOn(h -> "RoleBinding".equals(h.getKind())).singleElement().satisfies(h -> {
            if (h.getMetadata().getAnnotations() != null) {
                assertThat(h.getMetadata().getAnnotations()).doesNotContainKeys("prometheus.io/scrape", "prometheus.io/path",
                        "prometheus.io/port", "prometheus.io/scheme");
            }
        });
    }

}

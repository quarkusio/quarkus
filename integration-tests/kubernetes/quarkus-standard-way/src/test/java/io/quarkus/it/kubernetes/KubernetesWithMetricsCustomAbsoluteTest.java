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

import io.dekorate.prometheus.model.Endpoint;
import io.dekorate.prometheus.model.ServiceMonitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithMetricsCustomAbsoluteTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .overrideConfigKey("quarkus.http.port", "9090")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.path", "/absolute-metrics")
            .overrideConfigKey("quarkus.kubernetes.prometheus.prefix", "example.io")
            .overrideConfigKey("quarkus.kubernetes.prometheus.scrape", "example.io/should_be_scraped")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus", Version.getVersion())));

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
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("metrics");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getMetadata()).satisfies(meta -> {
                        // Annotations will have a different default prefix,
                        // and the scrape annotation was specifically configured
                        assertThat(meta.getAnnotations()).contains(entry("example.io/should_be_scraped", "true"),
                                entry("example.io/path", "/absolute-metrics"), entry("example.io/port", "9090"),
                                entry("example.io/scheme", "http"));
                    });
                });
            });

            assertThat(kubernetesList).filteredOn(i -> i.getKind().equals("ServiceMonitor")).singleElement()
                    .isInstanceOfSatisfying(ServiceMonitor.class, s -> {
                        assertThat(s.getMetadata()).satisfies(m -> {
                            assertThat(m.getName()).isEqualTo("metrics");
                        });

                        assertThat(s.getSpec()).satisfies(spec -> {
                            assertThat(spec.getEndpoints()).hasSize(1);
                            assertThat(spec.getEndpoints().get(0)).isInstanceOfSatisfying(Endpoint.class, e -> {
                                assertThat(e.getScheme()).isEqualTo("http");
                                assertThat(e.getTargetPort().getStrVal()).isNull();
                                assertThat(e.getTargetPort().getIntVal()).isEqualTo(9090);
                                assertThat(e.getPath()).isEqualTo("/absolute-metrics");
                            });
                        });
                    });
        });
    }

}

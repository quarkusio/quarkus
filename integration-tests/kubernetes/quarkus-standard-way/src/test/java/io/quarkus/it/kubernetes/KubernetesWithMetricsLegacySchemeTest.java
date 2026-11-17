package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * {@code quarkus.kubernetes.prometheus.scheme=https}, the setting earlier versions used to scrape over HTTPS, still makes
 * Prometheus scrape over HTTPS when {@code scrape-scheme} is not set.
 */
public class KubernetesWithMetricsLegacySchemeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics-legacy-scheme")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.http.port", "9090")
            .overrideConfigKey("quarkus.kubernetes.prometheus.scheme", "https")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getSpec().getTemplate().getMetadata().getAnnotations()).contains(
                    entry("prometheus.io/scrape", "true"),
                    entry("prometheus.io/path", "/q/metrics"),
                    entry("prometheus.io/port", "9090"),
                    entry("prometheus.io/scheme", "https"));
            assertThat(d.getSpec().getTemplate().getMetadata().getAnnotations()).doesNotContainKey("https");
        });

        assertThat(kubernetesList).filteredOn(i -> i.getKind().equals("ServiceMonitor")).singleElement()
                .isInstanceOfSatisfying(ServiceMonitor.class, s -> {
                    assertThat(s.getSpec().getEndpoints()).singleElement()
                            .isInstanceOfSatisfying(Endpoint.class, e -> {
                                assertThat(e.getScheme()).isEqualTo("https");
                                assertThat(e.getTargetPort().getIntVal()).isEqualTo(9090);
                                assertThat(e.getPath()).isEqualTo("/q/metrics");
                            });
                });
    }
}

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
 * With the management interface enabled, Prometheus scrapes the management port over HTTPS. The application HTTP server
 * does not need an https port.
 */
public class KubernetesWithMetricsHttpsAndManagementInterfaceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics-https-management")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.management.port", "9876")
            .overrideConfigKey("quarkus.management.ssl.certificate.files", "server.crt")
            .overrideConfigKey("quarkus.management.ssl.certificate.key-files", "server.key")
            .overrideConfigKey("quarkus.kubernetes.prometheus.scrape-scheme", "https")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList).filteredOn(h -> "Deployment".equals(h.getKind())).singleElement()
                .isInstanceOfSatisfying(Deployment.class, d -> {
                    assertThat(d.getSpec().getTemplate().getMetadata().getAnnotations()).contains(
                            entry("prometheus.io/scrape", "true"),
                            entry("prometheus.io/path", "/q/metrics"),
                            entry("prometheus.io/port", "9876"),
                            entry("prometheus.io/scheme", "https"));
                });

        assertThat(kubernetesList).filteredOn(i -> i.getKind().equals("ServiceMonitor")).singleElement()
                .isInstanceOfSatisfying(ServiceMonitor.class, s -> {
                    assertThat(s.getSpec().getEndpoints()).singleElement()
                            .isInstanceOfSatisfying(Endpoint.class, e -> {
                                assertThat(e.getScheme()).isEqualTo("https");
                                assertThat(e.getTargetPort().getIntVal()).isEqualTo(9876);
                                assertThat(e.getPath()).isEqualTo("/q/metrics");
                            });
                });
    }
}

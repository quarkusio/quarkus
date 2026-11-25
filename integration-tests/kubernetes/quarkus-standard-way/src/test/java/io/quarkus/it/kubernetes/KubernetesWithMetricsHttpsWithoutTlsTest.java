package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Scraping over HTTPS needs an https port. Without TLS or the management interface, the build fails instead of sending
 * Prometheus to the plain HTTP port.
 */
public class KubernetesWithMetricsHttpsWithoutTlsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics-https-without-tls")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.prometheus.scrape-scheme", "https")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus", Version.getVersion())))
            .assertBuildException(e -> assertThat(e)
                    .hasStackTraceContaining("Prometheus scrapes over HTTPS, but the generated resources have no https port"));

    @Test
    public void buildShouldFail() {
        fail("Build should have failed and therefore this method should not have been called");
    }
}

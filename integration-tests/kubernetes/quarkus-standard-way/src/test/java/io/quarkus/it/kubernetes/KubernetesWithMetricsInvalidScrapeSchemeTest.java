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
 * {@code quarkus.kubernetes.prometheus.scrape-scheme} accepts only {@code http} and {@code https}.
 */
public class KubernetesWithMetricsInvalidScrapeSchemeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("metrics-invalid-scheme")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.prometheus.scrape-scheme", "ftp")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus", Version.getVersion())))
            .assertBuildException(e -> assertThat(e)
                    .hasStackTraceContaining("quarkus.kubernetes.prometheus.scrape-scheme")
                    .hasStackTraceContaining("ftp"));

    @Test
    public void buildShouldFail() {
        fail("Build should have failed and therefore this method should not have been called");
    }
}

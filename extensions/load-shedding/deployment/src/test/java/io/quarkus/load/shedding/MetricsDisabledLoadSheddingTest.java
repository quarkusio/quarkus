package io.quarkus.load.shedding;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.Version;
import io.quarkus.load.shedding.runtime.LoadSheddingMetrics;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that no load shedding metrics are published when they are turned off.
 */
public class MetricsDisabledLoadSheddingTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class))
            .overrideConfigKey("quarkus.load-shedding.metrics.enabled", "false")
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-deployment", Version.getVersion())));

    @Inject
    MeterRegistry registry;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    public void test() {
        when().get("/").then().statusCode(200);

        assertThat(registry.getMeters())
                .extracting(meter -> meter.getId().getName())
                .doesNotContain(
                        LoadSheddingMetrics.SHED_REQUESTS,
                        LoadSheddingMetrics.CURRENT_REQUESTS,
                        LoadSheddingMetrics.CURRENT_LIMIT,
                        LoadSheddingMetrics.OVERLOAD_STATUS,
                        LoadSheddingMetrics.OVERLOAD_DEPTH,
                        LoadSheddingMetrics.OVERLOAD_DURATION);
    }

    @Path("/")
    public static class MyResource {
        @GET
        public String hello() {
            return "Hello, world!";
        }
    }
}

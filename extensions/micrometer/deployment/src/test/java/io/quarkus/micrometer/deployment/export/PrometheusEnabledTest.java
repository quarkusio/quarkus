package io.quarkus.micrometer.deployment.export;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.is;

import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;

public class PrometheusEnabledTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.http.enable-compression", "true")
            .withEmptyApplication();

    @Inject
    MeterRegistry registry;

    @Inject
    PrometheusMeterRegistry promRegistry;

    @Test
    @Order(Integer.MIN_VALUE)
    public void testMeterRegistryPresent() {
        // Prometheus is enabled (only registry)
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        Assertions.assertEquals(1, subRegistries.size(),
                "There should be only one configured subregistry. Found " + subRegistries);

        PrometheusMeterRegistry subPromRegistry = (PrometheusMeterRegistry) subRegistries.iterator().next();
        Assertions.assertEquals(PrometheusMeterRegistry.class, subPromRegistry.getClass(),
                "Should be PrometheusMeterRegistry");
        Assertions.assertEquals(subPromRegistry, promRegistry,
                "Should be the same bean as the injected PrometheusMeterRegistry");
    }

    @Test
    public void metricsEndpoint() {
        // RestAssured prepends /app for us
        given()
                .accept("application/json")
                .get("/q/metrics")
                .then()
                .log().all()
                .statusCode(406);

        given()
                .get("/q/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    public void metricsEndpointCompressed() {
        // Register a metric so the scrape response is non-empty;
        // Vert.x/Netty skips compression on empty bodies.
        promRegistry.counter("test.compression.verify").increment();

        // Use DecoderConfig instead of .header("Accept-Encoding", "gzip")
        // because RestAssured silently ignores manually set Accept-Encoding headers.
        // See CompressionTest and Testflow for details.
        given().config(RestAssured.config
                .decoderConfig(DecoderConfig.decoderConfig()
                        .contentDecoders(DecoderConfig.ContentDecoder.GZIP)))
                .get("/q/metrics")
                .then()
                .log().all()
                .statusCode(200)
                .header("content-encoding", is("gzip"));
    }
}

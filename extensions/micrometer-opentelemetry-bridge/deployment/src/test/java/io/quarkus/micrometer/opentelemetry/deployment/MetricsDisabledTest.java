package io.quarkus.micrometer.opentelemetry.deployment;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.PingPongResource;
import io.quarkus.micrometer.opentelemetry.deployment.common.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MetricsDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Util.class,
                                    PingPongResource.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset("""
                                    quarkus.otel.sdk.disabled=true\n
                                    quarkus.otel.metrics.enabled=true\n
                                    quarkus.otel.traces.exporter=none\n
                                    quarkus.otel.logs.exporter=none\n
                                    quarkus.otel.metrics.exporter=in-memory\n
                                    quarkus.otel.metric.export.interval=300ms\n
                                    quarkus.micrometer.binder.http-client.enabled=true\n
                                    quarkus.micrometer.binder.http-server.enabled=true\n
                                    pingpong/mp-rest/url=${test.url}\n
                                    quarkus.redis.devservices.enabled=false\n
                                    """),
                                    "application.properties"));

    @Inject
    protected InMemoryMetricExporter metricExporter;

    protected static String mapToString(Map<AttributeKey<?>, ?> map) {
        return (String) map.keySet().stream()
                .map(key -> "" + key.getKey() + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @BeforeEach
    void setUp() {
        metricExporter.reset();
    }

    @Test
    void disabledTest() throws InterruptedException {
        // The otel metrics are disabled
        RestAssured.basePath = "/";
        when().get("/ping/one").then().statusCode(200);

        Thread.sleep(200);

        List<MetricData> metricData = metricExporter.getFinishedMetricItems();
        assertThat(metricData).isEmpty();
    }
}

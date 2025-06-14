package io.quarkus.opentelemetry.deployment.metrics;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Validate all JVM metrics being produced.
 */
public class JvmMetricsTest extends BaseJvmMetricsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.metrics.enabled=true\n" +
                                            "quarkus.otel.traces.exporter=none\n" +
                                            "quarkus.otel.logs.exporter=none\n" +
                                            "quarkus.otel.metrics.exporter=in-memory\n" +
                                            "quarkus.otel.metric.export.interval=300ms\n"),
                                    "application.properties"));

    @Test
    void allMetrics() throws InterruptedException {
        Set<MetricToAssert> allMetrics = Set.of(
                new JvmMetricsTest.MetricToAssert("http.server.request.duration", "Duration of HTTP server requests.", "s",
                        HISTOGRAM), // just because we generate load with HTTP
                new JvmMetricsTest.MetricToAssert("jvm.memory.committed", "Measure of memory committed.", "By", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.memory.used", "Measure of memory used.", "By", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.memory.limit", "Measure of max obtainable memory.", "By", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.memory.used_after_last_gc",
                        "Measure of memory used, as measured after the most recent garbage collection event on this pool.",
                        "By", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.gc.duration", "Duration of JVM garbage collection actions.", "s",
                        HISTOGRAM),
                new JvmMetricsTest.MetricToAssert("jvm.class.count", "Number of classes currently loaded.", "{class}",
                        LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.class.loaded", "Number of classes loaded since JVM start.", "{class}",
                        LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.class.unloaded", "Number of classes unloaded since JVM start.",
                        "{class}", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.count",
                        "Number of processors available to the Java virtual machine.", "{cpu}", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.limit", "", "1", LONG_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.time", "CPU time used by the process as reported by the JVM.", "s",
                        DOUBLE_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.recent_utilization",
                        "Recent CPU utilization for the process as reported by the JVM.", "1", DOUBLE_GAUGE),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.longlock", "Long lock times", "s", HISTOGRAM),
                new JvmMetricsTest.MetricToAssert("jvm.cpu.context_switch", "", "Hz", DOUBLE_SUM),
                new JvmMetricsTest.MetricToAssert("jvm.network.io", "Network read/write bytes.", "By", HISTOGRAM), //
                new JvmMetricsTest.MetricToAssert("jvm.network.time", "Network read/write duration.", "s", HISTOGRAM), //
                new JvmMetricsTest.MetricToAssert("jvm.thread.count", "Number of executing platform threads.", "{thread}",
                        LONG_SUM));

        // Force GC to run
        System.gc();

        // only to get some load
        RestAssured.when()
                .get("/span").then()
                .statusCode(200)
                .body(is("hello"));

        Awaitility.await().atMost(10, SECONDS)
                .untilAsserted(() -> assertEquals(allMetrics.size(),
                        metricExporter.getFinishedMetricItems().stream()
                                .map(MetricData::getName)
                                .collect(Collectors.toSet())
                                .size(),
                        "Found: " + metricExporter.getFinishedMetricItems().stream()
                                .map(MetricData::getName)
                                .collect(Collectors.toSet())));

        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems();

        Set<String> foundMetricNames = finishedMetricItems.stream()
                .map(MetricData::getName)
                .collect(Collectors.toSet());

        assertThat(foundMetricNames).isEqualTo(allMetrics.stream()
                .map(MetricToAssert::name)
                .collect(Collectors.toSet()));

        allMetrics.forEach(metric -> {
            assertMetric(metric);
        });
    }

    @Path("/")
    public static class SpanResource {
        @GET
        @Path("/span")
        public Response span() {
            return Response.ok("hello").build();
        }
    }
}

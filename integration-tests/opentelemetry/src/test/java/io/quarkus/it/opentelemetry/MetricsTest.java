package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MetricsTest {
    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (!spans.isEmpty()) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.isEmpty();
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .when()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

    @Test
    public void directCounterTest() {
        given()
                .when()
                .get("/direct-metrics")
                .then()
                .statusCode(200);
        given()
                .when().get("/direct-metrics")
                .then()
                .statusCode(200);

        await().atMost(10, SECONDS).until(() -> getSpans().size() >= 2);
        assertEquals(2, getSpans().size(), () -> "The spans are " + getSpans());
        await().atMost(10, SECONDS).until(() -> getMetrics("direct-trace-counter").size() > 2);

        List<Map<String, Object>> metrics = getMetrics("direct-trace-counter");
        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics("direct-trace-counter")
                .get(metrics.size() - 1)
                .get("longSumData")))
                .get("points"))
                .get(0))
                .get("value");

        assertEquals(2, value, "received: " + given()
                .when()
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                }));
    }

    @Test
    void testAllJvmMetrics() {
        // Force GC to run
        System.gc();

        // generate load
        given()
                .when()
                .get("/nopath")
                .then()
                .statusCode(200);

        Set<MetricToAssert> allMetrics = getJvmMetricsToAssert();

        await().atMost(10, SECONDS).untilAsserted(() -> {
            Set<String> allMetricNames = getAllMetricNames("jvm.");
            assertThat(allMetricNames.size())
                    .withFailMessage("The jvm metrics are " + allMetricNames)
                    .isGreaterThanOrEqualTo(allMetrics.size());
        });

        //        System.out.println(getAllMetricNames("jvm."));

        allMetrics.forEach(metricToAssert -> {

            await().atMost(10, SECONDS)
                    .untilAsserted(() -> assertThat(getMetrics(metricToAssert.name()).size())
                            .withFailMessage("The metric " + metricToAssert.name())
                            .isGreaterThan(0));

            List<Map<String, Object>> metrics = getMetrics(metricToAssert.name());

            assertThat(metrics.size())
                    .withFailMessage(metricToAssert.name() + " not found")
                    .isGreaterThan(0);

            Map<String, Object> lastMetric = metrics.size() > 0 ? metrics.get(metrics.size() - 1) : null;

            Double value = ((Number) ((Map) ((List) ((Map) (lastMetric.get("data")))
                    .get("points"))
                    .get(0))
                    .get(metricToAssert.metricType().equals(HISTOGRAM) ? "sum" : "value"))
                    .doubleValue();

            if (!metricToAssert.name().equals("jvm.memory.used_after_last_gc") &&
                    !metricToAssert.name().equals("jvm.cpu.limit") &&
                    !metricToAssert.name().equals("jvm.cpu.recent_utilization") && // skip value assertions on flaky metrics
                    !metricToAssert.name().equals("jvm.system.cpu.utilization")) {
                assertThat(value)
                        .withFailMessage("Metric should be greater than 0: " + metricToAssert.name + " value: " + value)
                        .isGreaterThan(0d);
            }
        });
    }

    protected Set<MetricToAssert> getJvmMetricsToAssert() {
        return Set.of(
                //                new MetricToAssert("http.server.request.duration", "Duration of HTTP server requests.", "s",
                //                        HISTOGRAM), // just because we generate load with HTTP
                new MetricToAssert("jvm.memory.committed", "Measure of memory committed.", "By", LONG_SUM),
                new MetricToAssert("jvm.memory.used", "Measure of memory used.", "By", LONG_SUM),
                // Not on native
                new MetricToAssert("jvm.memory.limit", "Measure of max obtainable memory.", "By", LONG_SUM),
                new MetricToAssert("jvm.memory.used_after_last_gc",
                        "Measure of memory used, as measured after the most recent garbage collection event on this pool.",
                        "By", LONG_SUM),
                // not on native
                new MetricToAssert("jvm.gc.duration", "Duration of JVM garbage collection actions.", "s",
                        HISTOGRAM),
                new MetricToAssert("jvm.class.count", "Number of classes currently loaded.", "{class}",
                        LONG_SUM),
                new MetricToAssert("jvm.class.loaded", "Number of classes loaded since JVM start.", "{class}",
                        LONG_SUM),
                new MetricToAssert("jvm.class.unloaded", "Number of classes unloaded since JVM start.",
                        "{class}", LONG_SUM),
                new MetricToAssert("jvm.cpu.count",
                        "Number of processors available to the Java virtual machine.", "{cpu}", LONG_SUM),
                new MetricToAssert("jvm.cpu.limit", "", "1", LONG_SUM),
                //jvm.system.cpu.utilization instead, on native
                new MetricToAssert("jvm.cpu.time", "CPU time used by the process as reported by the JVM.", "s",
                        DOUBLE_SUM),
                new MetricToAssert("jvm.cpu.recent_utilization",
                        "Recent CPU utilization for the process as reported by the JVM.", "1", DOUBLE_GAUGE),
                new MetricToAssert("jvm.cpu.longlock", "Long lock times", "s", HISTOGRAM),
                new MetricToAssert("jvm.cpu.context_switch", "", "Hz", DOUBLE_SUM),
                // not on native
                new MetricToAssert("jvm.network.io", "Network read/write bytes.", "By", HISTOGRAM),
                new MetricToAssert("jvm.network.time", "Network read/write duration.", "s", HISTOGRAM),
                new MetricToAssert("jvm.thread.count", "Number of executing platform threads.", "{thread}",
                        LONG_SUM));
    }

    @Test
    void testServerRequestDuration() {
        given()
                .when()
                .get("/nopath")
                .then()
                .statusCode(200);

        await().atMost(10, SECONDS).until(() -> getMetrics("http.server.request.duration").size() > 2);

        List<Map<String, Object>> metrics = getMetrics("http.server.request.duration");

        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics("http.server.request.duration")
                .get(metrics.size() - 1)
                .get("data")))
                .get("points"))
                .get(0))
                .get("count");

        assertThat(value).isGreaterThanOrEqualTo(1); // at least one endpoint was called once
    }

    private static Set<String> getAllMetricNames(String prefix) {
        List<Map<String, Object>> foundMetrics = given()
                .when()
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });

        return foundMetrics.stream()
                .filter(m -> ((String) m.get("name")).startsWith(prefix))
                .map(m -> ((String) m.get("name")))
                .collect(Collectors.toSet());
    }

    record MetricToAssert(String name, String description, String metricUnit, MetricDataType metricType) {
    }
}

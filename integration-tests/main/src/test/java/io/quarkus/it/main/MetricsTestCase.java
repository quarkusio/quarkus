package io.quarkus.it.main;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MetricsTestCase {
    private static final String metricsPrefix = "application_io_quarkus_it_metrics_";

    @Test
    public void testCounter() {
        assertMetricExactValue(metricsPrefix + "MetricsResource_a_counted_resource_total", "0.0");
        invokeCounter();
        assertMetricExactValue(metricsPrefix + "MetricsResource_a_counted_resource_total", "1.0");
    }

    @Test
    public void testGauge() {
        invokeGauge();
        assertMetricExactValue(metricsPrefix + "MetricsResource_gauge", "42.0");
    }

    @Test
    public void testConcurrentGauge() throws InterruptedException {
        try {
            RestAssured.when().get("/metricsresource/cgauge");
            RestAssured.when().get("/metricsresource/cgauge");
            RestAssured.when().get("/metricsresource/cgauge");
            RestAssured.when().get("/metricsresource/cgauge");
            TimeUnit.SECONDS.sleep(2);
            assertMetricExactValue(metricsPrefix + "MetricsResource_cgauge_current", "4.0");
        } finally {
            RestAssured.when().get("/metricsresource/cgauge_finish");
        }
    }

    @Test
    public void testMeter() {
        invokeMeter();
        assertMetricExactValue(metricsPrefix + "MetricsResource_meter_total", "1.0");
        assertMetricExists(metricsPrefix + "MetricsResource_meter_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_meter_one_min_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_meter_five_min_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_meter_fifteen_min_rate_per_second");
    }

    @Test
    public void testTimer() {
        invokeTimer();
        assertMetricExactValue(metricsPrefix + "MetricsResource_timer_metric_seconds_count", "1.0");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.5\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.75\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.95\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.98\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.99\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_seconds{quantile=\"0.999\"}");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_one_min_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_five_min_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_fifteen_min_rate_per_second");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_min_seconds");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_max_seconds");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_mean_seconds");
        assertMetricExists(metricsPrefix + "MetricsResource_timer_metric_stddev_seconds");
    }

    @Test
    public void testHistogram() {
        invokeHistogram();
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram_count", "1.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram_min", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram_max", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram_mean", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram_stddev", "0.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.5\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.75\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.95\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.98\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.99\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "MetricsResource_histogram{quantile=\"0.999\"}", "42.0");
    }

    @Test
    public void testScopes() {
        RestAssured.when().get("/metrics/base").then().statusCode(200);
        RestAssured.when().get("/metrics/vendor").then().statusCode(200);
        RestAssured.when().get("/metrics/application").then().statusCode(200);
    }

    @Test
    public void testMetricWithAbsoluteName() {
        invokeCounterWithAbsoluteName();
        assertMetricExactValue("application_counter_absolute_total", "1.0");
    }

    @Test
    public void testMetricWithCustomTags() {
        invokeCounterWithTags();
        assertMetricExactValue(metricsPrefix + "MetricsResource_counter_with_tags_total{foo=\"bar\"}", "1.0");
    }

    @Test
    public void testInvalidScopes() {
        RestAssured.when().get("/metrics/foo").then().statusCode(404)
                .body(containsString("Bad scope requested"));
        RestAssured.when().get("/metrics/vendor/foo").then().statusCode(404)
                .body(containsString("Metric vendor/foo not found"));
    }

    @Test
    public void testBaseMetrics() {
        RestAssured.when().get("/metrics/base").then().statusCode(200)
                // the spaces at the end are there on purpose to make sure the metrics are named exactly this way
                .body(containsString("base_classloader_loadedClasses_total "))
                .body(containsString("base_cpu_systemLoadAverage "))
                .body(containsString("base_thread_count_total "))
                .body(containsString("base_classloader_loadedClasses_count "))
                .body(containsString("base_jvm_uptime_seconds "))
                .body(containsString("base_thread_max_count_total "))
                .body(containsString("base_memory_committedHeap_bytes "))
                .body(containsString("base_cpu_availableProcessors "))
                .body(containsString("base_thread_daemon_count_total "))
                .body(containsString("base_classloader_unloadedClasses_total "))
                .body(containsString("base_memory_maxHeap_bytes "))
                .body(containsString("base_memory_usedHeap_bytes "));
    }

    @Test
    public void testVendorMetrics() {
        RestAssured.when().get("/metrics/vendor").then().statusCode(200)
                // the spaces at the end are there on purpose to make sure the metrics are named exactly this way
                .body(containsString("vendor_memory_committedNonHeap_bytes "))
                .body(containsString("vendor_memory_usedNonHeap_bytes "))
                .body(containsString("vendor_memory_maxNonHeap_bytes "));
    }

    /**
     * A REST method with metrics is throwing a NotFoundException, so the client should receive 404.
     */
    @Test
    public void testEndpointWithMetricsThrowingException() {
        RestAssured.when().get("/metricsresource/counter-throwing-not-found-exception").then()
                .statusCode(404);
    }

    /**
     * Verify that no metrics are created from SmallRye internal classes (for example the
     * io.smallrye.metrics.interceptors package)
     */
    @Test
    public void testNoMetricsFromSmallRyeInternalClasses() {
        RestAssured.when().get("/metrics/application").then()
                .body(not(containsString("io_smallrye_metrics")));
    }

    private void assertMetricExactValue(String name, String val) {
        RestAssured.when().get("/metrics").then()
                .body(containsString(name + " " + val));
    }

    private void assertMetricExists(String name) {
        RestAssured.when().get("/metrics").then()
                .body(containsString(name));
    }

    public void invokeCounter() {
        RestAssured.when().get("/metricsresource/counter").then()
                .body(is("TEST"));
    }

    public void invokeGauge() {
        RestAssured.when().get("/metricsresource/gauge").then()
                .body(is("42"));
    }

    public void invokeConcurrentGauge() {
        RestAssured.when().get("/metricsresource/cgauge").then()
                .body(is("OK"));
    }

    public void invokeMeter() {
        RestAssured.when().get("/metricsresource/meter").then()
                .body(is("OK"));
    }

    public void invokeTimer() {
        RestAssured.when().get("/metricsresource/timer").then()
                .body(is("OK"));
    }

    public void invokeHistogram() {
        RestAssured.when().get("/metricsresource/histogram").then()
                .body(is("OK"));
    }

    public void invokeCounterWithAbsoluteName() {
        RestAssured.when().get("/metricsresource/counter-absolute").then()
                .body(is("TEST"));
    }

    public void invokeCounterWithTags() {
        RestAssured.when().get("/metricsresource/counter-with-tags").then()
                .body(is("TEST"));
    }

}

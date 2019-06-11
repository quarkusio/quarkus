package io.quarkus.it.main;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MetricsTestCase {
    private static final String metricsPrefix = "application:io_quarkus_it_metrics_";

    @Test
    public void testCounter() {
        invokeCounter();
        assertMetricExactValue(metricsPrefix + "metrics_resource_a_counted_resource", "1.0");
    }

    @Test
    public void testGauge() {
        invokeGauge();
        assertMetricExactValue(metricsPrefix + "metrics_resource_gauge", "42.0");
    }

    @Test
    public void testMeter() {
        invokeMeter();
        assertMetricExactValue(metricsPrefix + "metrics_resource_meter_total", "1.0");
        assertMetricExists(metricsPrefix + "metrics_resource_meter_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_meter_one_min_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_meter_five_min_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_meter_fifteen_min_rate_per_second");
    }

    @Test
    public void testTimer() {
        invokeTimer();
        assertMetricExactValue(metricsPrefix + "metrics_resource_timer_metric_seconds_count", "1.0");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.5\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.75\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.95\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.98\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.99\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_seconds{quantile=\"0.999\"}");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_one_min_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_five_min_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_fifteen_min_rate_per_second");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_min_seconds");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_max_seconds");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_mean_seconds");
        assertMetricExists(metricsPrefix + "metrics_resource_timer_metric_stddev_seconds");
    }

    @Test
    public void testHistogram() {
        invokeHistogram();
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram_count", "1.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram_min", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram_max", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram_mean", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram_stddev", "0.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.5\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.75\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.95\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.98\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.99\"}", "42.0");
        assertMetricExactValue(metricsPrefix + "metrics_resource_histogram{quantile=\"0.999\"}", "42.0");
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
        assertMetricExactValue("application:counter_absolute", "1.0");
    }

    @Test
    public void testMetricWithCustomTags() {
        invokeCounterWithTags();
        assertMetricExactValue(metricsPrefix + "metrics_resource_counter_with_tags{foo=\"bar\"}", "1.0");
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
                .body(containsString("base:classloader_total_loaded_class_count "))
                .body(containsString("base:cpu_system_load_average "))
                .body(containsString("base:thread_count "))
                .body(containsString("base:classloader_current_loaded_class_count "))
                .body(containsString("base:jvm_uptime_seconds "))
                .body(containsString("base:thread_max_count "))
                .body(containsString("base:memory_committed_heap_bytes "))
                .body(containsString("base:cpu_available_processors "))
                .body(containsString("base:thread_daemon_count "))
                .body(containsString("base:classloader_total_unloaded_class_count "))
                .body(containsString("base:memory_max_heap_bytes "))
                .body(containsString("base:memory_used_heap_bytes "));
    }

    @Test
    public void testVendorMetrics() {
        RestAssured.when().get("/metrics/vendor").then().statusCode(200)
                // the spaces at the end are there on purpose to make sure the metrics are named exactly this way
                .body(containsString("vendor:memory_committed_non_heap_bytes "))
                .body(containsString("vendor:memory_used_non_heap_bytes "))
                .body(containsString("vendor:memory_max_non_heap_bytes "));
    }

    /**
     * A REST method with metrics is throwing a NotFoundException, so the client should receive 404.
     */
    @Test
    public void testEndpointWithMetricsThrowingException() {
        RestAssured.when().get("/metricsresource/counter-throwing-not-found-exception").then()
                .statusCode(404);
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

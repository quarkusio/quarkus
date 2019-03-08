/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.example.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MetricsTestCase {

    @Test
    public void testCounter() {
        invokeCounter();
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_a_counted_resource", "1.0");
    }

    @Test
    public void testGauge() {
        invokeGauge();
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_gauge", "42.0");
    }

    @Test
    public void testMeter() {
        invokeMeter();
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_meter_total", "1.0");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_meter_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_meter_one_min_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_meter_five_min_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_meter_fifteen_min_rate_per_second");
    }

    @Test
    public void testTimer() {
        invokeTimer();
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds_count", "1.0");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.5\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.75\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.95\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.98\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.99\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_seconds{quantile=\"0.999\"}");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_one_min_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_five_min_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_fifteen_min_rate_per_second");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_min_seconds");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_max_seconds");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_mean_seconds");
        assertMetricExists("application:io_quarkus_example_metrics_metrics_resource_timer_metric_stddev_seconds");
    }

    @Test
    public void testHistogram() {
        invokeHistogram();
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram_count", "1.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram_min", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram_max", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram_mean", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram_stddev", "0.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.5\"}", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.75\"}", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.95\"}", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.98\"}", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.99\"}", "42.0");
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_histogram{quantile=\"0.999\"}", "42.0");
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
        assertMetricExactValue("application:io_quarkus_example_metrics_metrics_resource_counter_with_tags{foo=\"bar\"}", "1.0");
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

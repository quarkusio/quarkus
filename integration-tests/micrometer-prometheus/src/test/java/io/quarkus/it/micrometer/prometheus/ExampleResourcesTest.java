package io.quarkus.it.micrometer.prometheus;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** See Micrometer Guide */
@QuarkusTest
public class ExampleResourcesTest {

    @Test
    void testGaugeExample() {
        when().get("/example/gauge/1").then().statusCode(200);
        when().get("/example/gauge/2").then().statusCode(200);
        when().get("/example/gauge/4").then().statusCode(200);
        assertMetrics(when().get("/q/metrics").then().statusCode(200).extract().asInputStream())
                .hasMetricWithExactLabelsAndValue("example_list_size", 2.0,
                        entry("env", "test"), entry("env2", "test"), entry("registry", "prometheus"));
        when().get("/example/gauge/6").then().statusCode(200);
        when().get("/example/gauge/5").then().statusCode(200);
        when().get("/example/gauge/7").then().statusCode(200);
        assertMetrics(when().get("/q/metrics").then().statusCode(200).extract().asInputStream())
                .hasMetricWithExactLabelsAndValue("example_list_size", 1.0,
                        entry("env", "test"), entry("env2", "test"), entry("registry", "prometheus"));
    }

    @Test
    void testCounterExample() {
        when().get("/example/prime/-1").then().statusCode(200);
        when().get("/example/prime/0").then().statusCode(200);
        when().get("/example/prime/1").then().statusCode(200);
        when().get("/example/prime/2").then().statusCode(200);
        when().get("/example/prime/3").then().statusCode(200);
        when().get("/example/prime/15").then().statusCode(200);

        assertMetrics(when().get("/q/metrics").then().statusCode(200).extract().asInputStream())
                .hasMetricWithExactLabels("example_prime_number_total",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("type", "prime"))
                .hasMetricWithExactLabels("example_prime_number_total",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("type", "not-prime"))
                .hasMetricWithExactLabels("example_prime_number_total",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("type", "one"))
                .hasMetricWithExactLabels("example_prime_number_total",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("type", "even"))
                .hasMetricWithExactLabels("example_prime_number_total",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("type", "not-natural"));
    }

    @Test
    void testTimerExample() {
        when().get("/example/prime/257").then().statusCode(200);
        assertMetrics(when().get("/q/metrics").then().statusCode(200).extract().asInputStream())
                .hasMetricWithExactLabels("example_prime_number_test_seconds_sum",
                        entry("env", "test"), entry("env2", "test"),
                        entry("prime", "true"), entry("registry", "prometheus"))
                .hasMetricWithExactLabels("example_prime_number_test_seconds_max",
                        entry("env", "test"), entry("env2", "test"),
                        entry("prime", "true"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("example_prime_number_test_seconds_count", 1.0,
                        entry("env", "test"), entry("env2", "test"),
                        entry("prime", "true"), entry("registry", "prometheus"));
        when().get("/example/prime/7919").then().statusCode(200);
        assertMetrics(when().get("/q/metrics").then().statusCode(200).extract().asInputStream())
                .hasMetricWithExactLabelsAndValue("example_prime_number_test_seconds_count", 2.0,
                        entry("env", "test"), entry("env2", "test"),
                        entry("prime", "true"), entry("registry", "prometheus"));
    }
}

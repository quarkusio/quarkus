package io.quarkus.it.metrics;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MetricsOnClassTestCase {

    @Test
    public void testCounterOnMethod() {
        assertMetricExactValue("application", "foo.method", "application_foo_method_total", "0.0");
        RestAssured.when().get("/metricsonclass/method");
        assertMetricExactValue("application", "foo.method", "application_foo_method_total", "1.0");
    }

    @Test
    public void testCounterOnConstructor() {
        assertMetricExists("application", "foo.MetricsOnClassResource", "application_foo_MetricsOnClassResource_total");
    }

    private void assertMetricExactValue(String scope, String name, String expectedNameInOutput, String val) {
        RestAssured.when().get("/q/metrics/" + scope + "/" + name).then()
                .body(containsString(expectedNameInOutput + " " + val));
    }

    private void assertMetricExists(String scope, String name, String expectedNameInOutput) {
        RestAssured.when().get("/q/metrics/" + scope + "/" + name).then()
                .body(containsString(expectedNameInOutput + " "));
    }

}

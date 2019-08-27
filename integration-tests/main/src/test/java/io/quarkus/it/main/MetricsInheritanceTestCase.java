package io.quarkus.it.main;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MetricsInheritanceTestCase {

    @Test
    public void verifyRegistrations() {
        RestAssured.when().get("/metricsinheritanceresource/registration")
                .then().body("", Matchers.containsInAnyOrder(
                        "io.quarkus.it.metrics.inheritance.InheritanceMetricsBase.InheritanceMetricsBase",
                        "io.quarkus.it.metrics.inheritance.InheritanceMetricsBase.baseMethod",
                        "io.quarkus.it.metrics.inheritance.InheritanceMetricsExtended.InheritanceMetricsExtended",
                        "io.quarkus.it.metrics.inheritance.InheritanceMetricsExtended.anotherMethod",
                        "io.quarkus.it.metrics.inheritance.InheritanceMetricsExtended.baseMethod"));
    }

}

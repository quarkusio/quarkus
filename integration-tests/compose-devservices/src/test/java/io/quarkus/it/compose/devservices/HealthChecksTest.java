package io.quarkus.it.compose.devservices;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HealthChecksTest {

    @Test
    public void health() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.name",
                        containsInAnyOrder(
                                "Redis connection health check",
                                "Kafka connection health check",
                                "Database connections health check",
                                "SmallRye Reactive Messaging - readiness check"));
    }

}

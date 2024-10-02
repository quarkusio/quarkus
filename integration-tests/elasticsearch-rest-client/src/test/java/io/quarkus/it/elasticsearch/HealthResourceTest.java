package io.quarkus.it.elasticsearch;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HealthResourceTest {

    @Test
    public void testHealth() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Elasticsearch cluster health check"));
    }
}

package io.quarkus.it.reactive.mssql.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class HealthCheckTest {
    @Test
    public void testHealthCheck() {
        RestAssured.when().get("/q/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Reactive MS SQL connections health check"));
    }
}

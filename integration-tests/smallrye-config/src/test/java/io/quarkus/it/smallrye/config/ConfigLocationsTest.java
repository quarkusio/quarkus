package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigLocationsTest {
    @Test
    void locations() {
        given()
                .get("/config/{name}", "config.properties")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));

        given()
                .get("/config/{name}", "config.properties.common")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));
    }
}

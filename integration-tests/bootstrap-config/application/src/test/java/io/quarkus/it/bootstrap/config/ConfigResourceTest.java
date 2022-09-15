package io.quarkus.it.bootstrap.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConfigResourceTest {
    @Test
    void bootstrap() {
        given()
                .get("/config/{name}", "quarkus.dummy.name")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("foo"))
                .body("configSourceName", equalTo("bootstrap"));

        given()
                .get("/config/{name}", "quarkus.dummy.times")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("9"))
                .body("configSourceName", equalTo("bootstrap"));

        given()
                .get("/config/{name}", "quarkus.dummy.map.key")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("value"))
                .body("configSourceName", equalTo("bootstrap"));

        given()
                .get("/config/{name}", "quarkus.dummy.map.system")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("system"))
                .body("configSourceName", equalTo("bootstrap"));
    }

    @Test
    void locations() {
        given()
                .get("/config/{name}", "quarkus.dummy.map.locations")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("locations"))
                .body("configSourceName", equalTo("bootstrap"));
    }
}

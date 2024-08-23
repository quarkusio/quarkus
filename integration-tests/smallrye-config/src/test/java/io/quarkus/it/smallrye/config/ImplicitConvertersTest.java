package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ImplicitConvertersTest {
    @Test
    void optional() {
        given()
                .get("/implicit/converters/optional")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("converted"));
    }

    @Test
    void list() {
        given()
                .get("/implicit/converters/list")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("converted"));
    }

    @Test
    void map() {
        given()
                .get("/implicit/converters/map")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("converted"));
    }
}

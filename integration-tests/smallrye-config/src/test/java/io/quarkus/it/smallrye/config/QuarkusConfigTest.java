package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusConfigTest {
    @Test
    void uuid() {
        given()
                .get("/config/{name}", "quarkus.uuid")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", is(notNullValue()))
                .body("configSourceName", equalTo("DefaultValuesConfigSource"));

        given()
                .get("/config/uuid")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", is(notNullValue()))
                .body("configSourceName", equalTo("DefaultValuesConfigSource"));
    }
}

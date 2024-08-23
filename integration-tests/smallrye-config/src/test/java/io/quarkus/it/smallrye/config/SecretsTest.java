package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SecretsTest {
    @Test
    void secrets() {
        given()
                .get("/config/{name}", "my.secret")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("decoded"));
    }
}

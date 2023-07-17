package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConfigSourceInterceptorTest {
    @Test
    void interceptor() {
        given()
                .get("/config/{name}", "interceptor.play")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("pingpong"));
    }
}

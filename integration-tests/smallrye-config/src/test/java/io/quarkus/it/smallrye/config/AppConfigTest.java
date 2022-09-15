package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AppConfigTest {
    @Test
    void getToString() {
        given()
                .get("/app-config/toString")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("AppConfig{name=app, info=Info{count=10, alias=alias}}"));
    }
}

package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigurableExceptionMapperTest {
    @Test
    void exception() {
        given()
                .get("/exception")
                .then()
                .statusCode(OK.getStatusCode())
                .body(equalTo("This is an exception!"));
    }
}

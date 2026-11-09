package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ThrowableResourceTest {

    @Test
    public void exceptionIsSerialized() {
        given()
                .when().get("/throwable")
                .then()
                .statusCode(400)
                .body("code", equalTo("E1"))
                .body("message", equalTo("Invalid request"))
                .body("localizedMessage", equalTo("Invalid request"))
                .body("cause", nullValue())
                .body("suppressed.size()", equalTo(0))
                .body("stackTrace.size()", equalTo(1))
                .body("stackTrace[0].className", equalTo("org.acme.Service"))
                .body("stackTrace[0].methodName", equalTo("execute"))
                .body("stackTrace[0].fileName", equalTo("Service.java"))
                .body("stackTrace[0].lineNumber", equalTo(42));
    }

    @Test
    public void causeAndSuppressedExceptionsAreSerialized() {
        given()
                .when().get("/throwable/graph")
                .then()
                .statusCode(400)
                .body("code", equalTo("E1"))
                .body("cause.code", equalTo("E2"))
                .body("cause.message", equalTo("Cause"))
                .body("cause.stackTrace[0].lineNumber", equalTo(42))
                .body("suppressed.size()", equalTo(1))
                .body("suppressed[0].code", equalTo("E3"))
                .body("suppressed[0].message", equalTo("Suppressed"))
                .body("suppressed[0].stackTrace[0].lineNumber", equalTo(42));
    }
}

package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class MethodHandleResourceIT extends MethodHandleResourceTest {

    @Test
    public void testJacksonAccessUsesReflection() {
        given()
                .contentType("application/json")
                .body(REQUEST_BODY)
                .when().post("/method-handle")
                .then()
                .statusCode(200)
                .body("creatorInvocationStackTrace", hasItem("java.lang.reflect.Method.invoke"))
                .body("setterInvocationStackTrace", hasItem("java.lang.reflect.Method.invoke"))
                .body("serializationInvocationStackTrace", hasItem("java.lang.reflect.Method.invoke"));
    }
}

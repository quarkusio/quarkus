package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SpringControllerTest {

    @Test
    public void testJsonResult() {
        RestAssured.when().get("/greeting/json/hello").then()
                .body(containsString("hello"));
    }

    @Test
    public void testException() {
        RestAssured.when().get("/exception/first").then()
                .body(containsString("first"))
                .statusCode(500);
    }

    @Test
    public void testRuntimeException() {
        RestAssured.when().get("/exception/second").then()
                .body(isEmptyString())
                .statusCode(503);
    }
}

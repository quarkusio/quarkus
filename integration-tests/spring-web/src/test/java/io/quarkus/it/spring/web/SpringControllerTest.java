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
                .contentType("application/json")
                .body(containsString("hello"));
    }

    @Test
    public void testJsonResultFromResponseEntity() {
        RestAssured.when().get("/greeting/re/json/hello").then()
                .contentType("application/json")
                .body(containsString("hello"));
    }

    @Test
    public void testJsonResult2() {
        RestAssured.when().get("/greeting/json/hello?suffix=000").then()
                .contentType("application/json")
                .body(containsString("hello000"));
    }

    @Test
    public void testInvalidJsonInputAndResult() {
        RestAssured.given().contentType("application/json").body("{\"name\":\"\"}").post("/greeting/person").then()
                .statusCode(400);
    }

    @Test
    public void testJsonInputAndResult() {
        RestAssured.given().contentType("application/json").body("{\"name\":\"George\"}").post("/greeting/person").then()
                .contentType("application/json")
                .body(containsString("hello George"));
    }

    @Test
    public void testFirstResponseStatusHoldingException() {
        RestAssured.when().get("/exception/first").then()
                .contentType("text/plain")
                .body(containsString("first"))
                .statusCode(500);
    }

    @Test
    public void testSecondResponseStatusHoldingException() {
        RestAssured.when().get("/exception/second").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(503);
    }

    @Test
    public void testExceptionHandlerVoidReturnType() {
        RestAssured.when().get("/exception/void").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(400);
    }

    @Test
    public void testExceptionHandlerWithoutResponseStatusOnExceptionOrMethod() {
        RestAssured.when().get("/exception/unannotated").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(204);
    }

    @Test
    public void testExceptionHandlerResponseEntityType() {
        RestAssured.when().get("/exception/responseEntity").then()
                .contentType("application/json")
                .body(containsString("bad state"), containsString("responseEntity"))
                .statusCode(402);
    }

    @Test
    public void testExceptionHandlerPojoEntityType() {
        RestAssured.when().get("/exception/pojo").then()
                .contentType("application/json")
                .body(containsString("hello from error"))
                .statusCode(417);
    }

    @Test
    public void testRestControllerWithoutRequestMapping() {
        RestAssured.when().get("/hello").then()
                .body(containsString("hello"));
    }

    @Test
    public void testResponseEntityWithIllegalArgumentException() {
        RestAssured.when().get("/exception/re").then()
                .contentType("application/json")
                .body(containsString("hello from error"))
                .statusCode(402);
    }
}

package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ExceptionHandlingTest {

    @Test
    public void testUnhandledAnnotatedException() {
        RestAssured.when().get("/exception/unhandled/exception").then()
                .contentType("text/plain")
                .body(containsString("unhandled"))
                .statusCode(500);
    }

    @Test
    public void testUnhandledAnnotatedRuntimeException() {
        RestAssured.when().get("/exception/unhandled/runtime").then()
                .contentType("text/plain")
                .body(is(emptyString()))
                .statusCode(503);
    }

    @Test
    public void testHandledRuntimeException() {
        RestAssured.when().get("/exception/runtime").then()
                .contentType("text/plain")
                .body(is(emptyString()))
                .statusCode(400);
    }

    @Test
    public void testHandledRuntimeExceptionAsXml() {
        RestAssured.given().accept("application/xml")
                .when().get("/exception/runtime").then()
                .contentType("application/xml")
                .body(is(emptyString()))
                .statusCode(400);
    }

    @Test
    public void testHandledUnannotatedException() {
        RestAssured.when().get("/exception/unannotated").then()
                .contentType("text/plain")
                .body(is(emptyString()))
                .statusCode(204);
    }

    @Test
    public void testHandledUnannotatedExceptionAsXml() {
        RestAssured.given().accept("application/xml")
                .when().get("/exception/unannotated").then()
                .contentType("application/xml")
                .body(is(emptyString()))
                .statusCode(204);
    }

    @Test
    public void testResponseEntityWithResponseEntityException() {
        RestAssured.when().get("/exception/re/re").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .body(containsString("/exception/re/re"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testPojoWithResponseEntityException() {
        RestAssured.when().get("/exception/re/pojo").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .body(containsString("/exception/re/pojo"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testVoidWithResponseEntityException() {
        RestAssured.when().get("/exception/re/void").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .body(containsString("/exception/re/void"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testVoidWithResponseEntityExceptionAsXml() {
        RestAssured.given().accept("application/xml")
                .when().get("/exception/re/void").then()
                .contentType("application/xml")
                .body(containsString("bad state"))
                .body(containsString("/exception/re/void"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testVoidWithResponseEntityExceptionAsHardcodedXml() {
        RestAssured.given().accept("application/json")
                .when().get("/exception/re/void/xml").then()
                .contentType("application/xml")
                .body(containsString("bad state"))
                .body(containsString("/exception/re/void/xml"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testResponseEntityWithPojoException() {
        RestAssured.when().get("/exception/pojo/re").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .statusCode(417);
    }

    @Test
    public void testPojoWithPojoException() {
        RestAssured.when().get("/exception/pojo/pojo").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .statusCode(417);
    }

    @Test
    public void testVoidWithPojoException() {
        RestAssured.when().get("/exception/pojo/void").then()
                .contentType("application/json")
                .body(containsString("bad state"))
                .statusCode(417);
    }

    @Test
    public void testVoidWithPojoExceptionAsXml() {
        RestAssured.given().accept("application/xml")
                .when().get("/exception/pojo/void").then()
                .contentType("application/xml")
                .body(containsString("bad state"))
                .statusCode(417);
    }

    @Test
    public void testStringWithStringException() {
        RestAssured.when().get("/exception/string").then()
                .statusCode(418)
                .contentType("text/plain")
                .body(is("bad state"));
    }

    @Test
    public void testStringWithStringExceptionAsXml() {
        RestAssured.given().accept("application/xml")
                .when().get("/exception/string").then()
                .statusCode(418)
                .contentType("application/xml")
                .body(is("bad state"));
    }

    @Test
    public void testResponseStatusException() {
        RestAssured.when().get("/exception/responseStatusException").then()
                .statusCode(509)
                .contentType("text/plain")
                .body(containsString("bandwidth exceeded"));
    }
}

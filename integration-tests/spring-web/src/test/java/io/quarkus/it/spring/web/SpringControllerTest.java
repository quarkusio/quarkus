package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;

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
    public void testRestControllerWithoutRequestMapping() {
        RestAssured.when().get("/hello").then()
                .body(containsString("hello"));
    }
}

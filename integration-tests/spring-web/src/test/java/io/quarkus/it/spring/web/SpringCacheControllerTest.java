package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SpringCacheControllerTest {

    @Test
    public void testCache() {
        // first invocation
        RestAssured.when().get("/cache/greet/george").then()
                .contentType("application/json")
                .body(containsString("george"), containsString("0"));

        // second invocation should yield same count
        RestAssured.when().get("/cache/greet/george").then()
                .contentType("application/json")
                .body(containsString("george"), containsString("0"));

        // invocation with different key should yield different count
        RestAssured.when().get("/cache/greet/michael").then()
                .contentType("application/json")
                .body(containsString("michael"), containsString("1"));
    }
}

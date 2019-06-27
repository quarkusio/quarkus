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
                .body(containsString("hello"));
    }
}

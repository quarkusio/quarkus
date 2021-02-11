package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(GreetingController.class)
public class TestHTTPEndpointTest {

    @Test
    public void testJsonResult() {
        RestAssured.when().get("/json/hello").then()
                .contentType("application/json")
                .body(containsString("hello"));
    }
}

package io.quarkus.it.gcp.functions.http;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingTest {

    @Test
    public void testGreeting() {
        when().get("/hello").then().statusCode(200);

        when().get("/servlet/hello").then().statusCode(200);

        when().get("/vertx/hello").then().statusCode(200);

        when().get("/funqy").then().statusCode(200);
    }
}

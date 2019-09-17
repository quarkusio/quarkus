package io.quarkus.it.vertx.web;

import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Unit test for Function class.
 */
@QuarkusTest
public class VertxWebTest {

    @Test
    public void vertx() throws Exception {
        RestAssured.given().get("/vertx/hello").then().statusCode(200).body(IsEqual.equalTo("hello"));
        RestAssured.given().body("Joe").post("/vertx/hello").then().statusCode(200).body(IsEqual.equalTo("hello Joe"));

        RestAssured.given().get("/vertx/rx/hello").then().statusCode(200).body(IsEqual.equalTo("hello"));
        RestAssured.given().body("Joe").post("/vertx/rx/hello").then().statusCode(200).body(IsEqual.equalTo("hello Joe"));

        RestAssured.given().get("/vertx/exchange/hello").then().statusCode(200).body(IsEqual.equalTo("hello"));
    }

}

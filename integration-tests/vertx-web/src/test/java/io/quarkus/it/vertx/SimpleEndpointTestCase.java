package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SimpleEndpointTestCase {

    @Test
    public void testEndpoint() throws Exception {
        when().get("/simple/person").then().statusCode(200)
                .body("name", is("Jan"))
                .header("content-type", "application/json");
        when().get("/simple/pet").then().statusCode(200)
                .body("name", is("Jack"))
                .header("content-type", "application/json");
        when().get("/simple/pong").then().statusCode(200)
                .body("name", is("ping"))
                .header("content-type", "application/json");
        given().body("{\"name\":\"pi\"}").post("/simple/data").then().statusCode(200)
                .body("name", is("pipi"))
                .header("content-type", "application/json");
    }

}

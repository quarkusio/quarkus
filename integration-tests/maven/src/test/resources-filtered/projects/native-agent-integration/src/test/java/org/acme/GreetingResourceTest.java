package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class GreetingResourceTest
{
    @Test
    public void testKnownName()
    {
        given()
            .when().get("/hello/Alice")
            .then()
            .statusCode(200)
            .body(is("Hello Alice"));
    }

    @Test
    public void testUnknownName()
    {
        given()
            .when().get("/hello/Bob")
            .then()
            .statusCode(404);
    }
}
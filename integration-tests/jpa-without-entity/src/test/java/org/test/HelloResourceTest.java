package org.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/jpa-without-entity")
                .then()
                .statusCode(200)
                .body(is("hello without jpa"));
    }

}

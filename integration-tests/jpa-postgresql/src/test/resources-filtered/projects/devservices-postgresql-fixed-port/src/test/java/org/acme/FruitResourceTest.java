package org.acme;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FruitResourceTest {

    @Test
    void listFruits() {
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200);
    }
}

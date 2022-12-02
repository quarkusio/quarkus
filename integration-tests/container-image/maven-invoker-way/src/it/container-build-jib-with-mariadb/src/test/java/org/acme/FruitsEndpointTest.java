package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FruitsEndpointTest {

    @Test
    public void testListAllFruits() {
        given().accept("application/json")
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

}

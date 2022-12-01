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
                .body("$.size()", is(0));
        // This test is rather weak, because we don't pre-populate the mongo db before running the tests
        // There's not a nice out-of-band method for initialising the db, so we assume if we can connect the db, we are good
    }

}

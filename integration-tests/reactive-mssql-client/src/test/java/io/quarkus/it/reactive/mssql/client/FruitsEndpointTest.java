package io.quarkus.it.reactive.mssql.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FruitsEndpointTest {

    @Test
    public void testListAllFruits() {
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString("Orange"),
                        containsString("Pear"),
                        containsString("Apple"));
    }

}

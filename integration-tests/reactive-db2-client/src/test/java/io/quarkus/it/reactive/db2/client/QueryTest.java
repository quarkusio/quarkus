package io.quarkus.it.reactive.db2.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QueryTest {

    @Test
    public void testListAllFruits() {
        given()
                .when().get("/plants/fruits/")
                .then()
                .statusCode(200)
                .body(
                        containsString("Apple"),
                        containsString("Orange"),
                        containsString("Pear"));
    }

    @Test
    public void testListAllLegumes() {
        given()
                .when().get("/plants/legumes/")
                .then()
                .statusCode(200)
                .body(
                        containsString("Broccoli"),
                        containsString("Cumcumber"),
                        containsString("Leeks"));
    }

}

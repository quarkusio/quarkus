package org.acme.resteasyjackson;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class FruitResourceTest {

    @Test
    public void testList() {
        given()
                .when().get("/resteasy-jackson/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(3),
                        "name", containsInAnyOrder("Apple", "Pineapple", "Strawberry"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit", null));
    }

    @Test
    public void testAdd() {
        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("/resteasy-jackson/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(4),
                        "name", containsInAnyOrder("Apple", "Pineapple", "Strawberry", "Pear"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit", null, "Winter fruit"));

        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .delete("/resteasy-jackson/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(3),
                        "name", containsInAnyOrder("Apple", "Pineapple", "Strawberry"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit", null));
    }
}
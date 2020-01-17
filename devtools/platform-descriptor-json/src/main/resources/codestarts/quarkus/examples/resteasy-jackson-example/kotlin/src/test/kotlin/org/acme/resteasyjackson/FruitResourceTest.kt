package org.acme.resteasyjackson

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

@QuarkusTest
class FruitResourceTest {

    @Test
    fun testList() {
        given()
                .`when`()["/resteasy-jackson/fruits"]
                .then()
                .statusCode(200)
                .body("$.size()", `is`(3),
                        "name", Matchers.containsInAnyOrder("Apple", "Pineapple", "Strawberry"),
                        "description", Matchers.containsInAnyOrder("Winter fruit", "Tropical fruit", null))
    }

    @Test
    fun testAdd() {
        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .`when`()
                .post("/resteasy-jackson/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", `is`(4),
                        "name", Matchers.containsInAnyOrder("Apple", "Pineapple", "Strawberry", "Pear"),
                        "description", Matchers.containsInAnyOrder("Winter fruit", "Tropical fruit", null, "Winter fruit"))
        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .`when`()
                .delete("/resteasy-jackson/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", `is`(3),
                        "name", Matchers.containsInAnyOrder("Apple", "Pineapple", "Strawberry"),
                        "description", Matchers.containsInAnyOrder("Winter fruit", "Tropical fruit", null))
    }

}
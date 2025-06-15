package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public abstract class AbstractInjectResourcesMethodTest {

    @Test
    void shouldGetListOfItems() {
        given().accept("application/json").when().get("/call/resource/items").then().statusCode(200).and().body("id",
                contains(1, 2));
    }

    @Test
    void shouldCollectionByName() {
        given().accept("application/json").when().get("/call/resource/collectionByName/full collection").then()
                .statusCode(200).and().body("id", is("full")).and().body("name", is("full collection"));
    }
}

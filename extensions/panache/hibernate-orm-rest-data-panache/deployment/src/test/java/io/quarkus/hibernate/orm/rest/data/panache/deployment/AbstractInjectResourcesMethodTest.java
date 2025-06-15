package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

public abstract class AbstractInjectResourcesMethodTest {

    @Test
    void shouldGetListOfItems() {
        given().accept("application/json").when().get("/call/resource/items").then().statusCode(200).and().body("id",
                contains(1, 2));
    }
}

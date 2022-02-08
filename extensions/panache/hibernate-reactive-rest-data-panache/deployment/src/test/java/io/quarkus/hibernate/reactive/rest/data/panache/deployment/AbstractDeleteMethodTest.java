package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;

public abstract class AbstractDeleteMethodTest {

    @Test
    void shouldNotDeleteNonExistentObject() {
        when().delete("/items/100")
                .then().statusCode(404);
    }

    @Test
    void shouldDeleteObject() {
        when().delete("/items/1")
                .then().statusCode(204);
    }
}

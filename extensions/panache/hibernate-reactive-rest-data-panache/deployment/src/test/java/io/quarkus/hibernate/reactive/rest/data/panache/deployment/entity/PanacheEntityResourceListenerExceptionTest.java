package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class PanacheEntityResourceListenerExceptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class,
                            AbstractItem.class, Item.class, ItemsResource.class,
                            ForbiddenItemListener.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldReturnForbiddenWhenListenerThrowsForbiddenException() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"forbidden-item\", \"collection\": {\"id\": \"full\"}}")
                .when().post("/items")
                .then().statusCode(403);
    }
}

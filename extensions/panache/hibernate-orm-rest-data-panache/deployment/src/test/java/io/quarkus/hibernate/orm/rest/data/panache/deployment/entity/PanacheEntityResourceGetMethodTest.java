package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractGetMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourceGetMethodTest extends AbstractGetMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, ItemsResource.class,
                            EmptyListItem.class, EmptyListItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldCopyAdditionalMethodsAsResources() {
        given().accept("application/json")
                .when().get("/collections/name/full collection")
                .then().statusCode(200)
                .and().body("id", is("full"))
                .and().body("name", is("full collection"));
    }

    @Test
    void shouldReturnItemsForFullCollection() {
        given().accept("application/json")
                .when().get("/items?collection.id=full")
                .then().statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void shouldReturnNoItemsForEmptyCollection() {
        given().accept("application/json")
                .when().get("/items?collection.id=empty")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

}

package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractPostMethodTest;
import io.quarkus.test.QuarkusExtensionTest;

class PanacheEntityResourcePostMethodTest extends AbstractPostMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, ItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldCopyAdditionalMethodsAsResources() {
        given().accept("application/json")
                .when().post("/collections/name/mycollection")
                .then().statusCode(200)
                .and().body("id", is("mycollection"))
                .and().body("name", is("mycollection"));
    }
}

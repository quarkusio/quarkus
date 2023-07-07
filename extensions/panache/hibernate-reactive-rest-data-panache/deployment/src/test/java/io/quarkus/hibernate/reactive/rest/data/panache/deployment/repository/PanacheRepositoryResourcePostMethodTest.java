package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractPostMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheRepositoryResourcePostMethodTest extends AbstractPostMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class)
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

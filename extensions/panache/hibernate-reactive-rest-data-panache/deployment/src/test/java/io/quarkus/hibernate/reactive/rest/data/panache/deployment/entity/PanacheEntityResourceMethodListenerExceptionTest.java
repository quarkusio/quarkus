package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.RestDataResourceMethodListener;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourceMethodListenerExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, ItemsResource.class, WebApplicationExceptionResourceMethodListener.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldReceiveWebApplicationException() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-simple\", \"collection\": {\"id\": \"full\"}}")
                .when().post("/items")
                .then()
                .statusCode(403);
    }

    @ApplicationScoped
    public static class WebApplicationExceptionResourceMethodListener implements RestDataResourceMethodListener<Item> {

        @Override
        public void onBeforeAdd(Item item) {
            throw new ForbiddenException("You shall not pass");
        }
    }

}

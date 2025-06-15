package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PanacheEntityResourceMethodListenerTest {

    public static final AtomicInteger ON_BEFORE_SAVE_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger ON_AFTER_SAVE_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger ON_BEFORE_UPDATE_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger ON_AFTER_UPDATE_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger ON_BEFORE_DELETE_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger ON_AFTER_DELETE_COUNTER = new AtomicInteger(0);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                    Item.class, ItemsResource.class, ItemRestDataResourceMethodListener.class)
            .addAsResource("application.properties").addAsResource("import.sql"));

    @Order(1)
    @Test
    void shouldListenersBeCalledWhenCreatingEntities() {
        whenCreateEntity();
        assertEquals(1, ON_BEFORE_SAVE_COUNTER.get());
        assertEquals(1, ON_AFTER_SAVE_COUNTER.get());
    }

    @Order(2)
    @Test
    void shouldListenersBeCalledWhenUpdatingEntities() {
        whenUpdateEntity();
        assertEquals(1, ON_BEFORE_UPDATE_COUNTER.get());
        assertEquals(1, ON_AFTER_UPDATE_COUNTER.get());
    }

    @Order(3)
    @Test
    void shouldListenersBeCalledWhenDeletingEntities() {
        whenDeleteEntity();
        assertEquals(1, ON_BEFORE_DELETE_COUNTER.get());
        assertEquals(1, ON_AFTER_DELETE_COUNTER.get());
    }

    private void whenCreateEntity() {
        Response response = given().accept("application/json").and().contentType("application/json").and()
                .body("{\"name\": \"test-simple\", \"collection\": {\"id\": \"full\"}}").when().post("/items")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void whenUpdateEntity() {
        given().accept("application/json").and().contentType("application/json").and()
                .body("{\"id\": \"1\", \"name\": \"first-test\", \"collection\": {\"id\": \"full\"}}").when()
                .put("/items/1").then().statusCode(204);
    }

    private void whenDeleteEntity() {
        given().accept("application/json").and().contentType("application/json").and()
                .body("{\"id\": \"1\", \"name\": \"first-test\", \"collection\": {\"id\": \"full\"}}").when()
                .delete("/items/1").then().statusCode(204);
    }
}

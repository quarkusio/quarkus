package io.quarkus.it.hibernate.reactive.db2;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus when the session is injected without Uni or CompletionStage.
 */
@QuarkusTest
public class HibernateReactiveDB2AlternativeTest {

    @Test
    public void reactiveFind() {
        RestAssured.when()
                .get("/alternative-tests/reactiveFind")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactiveFindMutiny() {
        RestAssured.when()
                .get("/alternative-tests/reactiveFindMutiny")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactivePersist() {
        RestAssured.when()
                .get("/alternative-tests/reactivePersist")
                .then()
                .body(is("Tulip"));
    }

    @Test
    public void reactiveRemoveTransientEntity() {
        RestAssured.when()
                .get("/alternative-tests/reactiveRemoveTransientEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveRemoveManagedEntity() {
        RestAssured.when()
                .get("/alternative-tests/reactiveRemoveManagedEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveUpdate() {
        RestAssured.when()
                .get("/alternative-tests/reactiveUpdate")
                .then()
                .body(is("Tina"));
    }
}

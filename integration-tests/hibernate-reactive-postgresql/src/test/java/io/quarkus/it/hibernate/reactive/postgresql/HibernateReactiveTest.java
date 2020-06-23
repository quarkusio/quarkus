package io.quarkus.it.hibernate.reactive.postgresql;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 */
@QuarkusTest
public class HibernateReactiveTest {

    @Test
    public void reactiveFind() {
        RestAssured.when()
                .get("/tests/reactiveFind")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactiveCowPersist() {
        RestAssured.when()
                .get("/tests/reactiveCowPersist")
                .then()
                .body(containsString("\"name\":\"Carolina\"}")); //Use containsString as we don't know the Id this object will have
    }

    @Test
    public void reactiveFindMutiny() {
        RestAssured.when()
                .get("/tests/reactiveFindMutiny")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactivePersist() {
        RestAssured.when()
                .get("/tests/reactivePersist")
                .then()
                .body(is("Tulip"));
    }

    @Test
    public void reactiveRemoveTransientEntity() {
        RestAssured.when()
                .get("/tests/reactiveRemoveTransientEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveRemoveManagedEntity() {
        RestAssured.when()
                .get("/tests/reactiveRemoveManagedEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveUpdate() {
        RestAssured.when()
                .get("/tests/reactiveUpdate")
                .then()
                .body(is("Tina"));
    }
}

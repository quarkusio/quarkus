package io.quarkus.it.hibernate.reactive.postgresql;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 */
@QuarkusTest
@TestHTTPEndpoint(HibernateReactiveTestEndpoint.class)
public class HibernateReactiveTest {

    @Test
    public void reactiveCowPersist() {
        RestAssured.when()
                .get("/reactiveCowPersist")
                .then()
                .body(containsString("\"name\":\"Carolina\"}")); //Use containsString as we don't know the Id this object will have
    }

    @Test
    public void reactiveFindMutiny() {
        RestAssured.when()
                .get("/reactiveFindMutiny")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactivePersist() {
        RestAssured.when()
                .get("/reactivePersist")
                .then()
                .body(is("Tulip"));
    }

    @Test
    public void reactiveRemoveTransientEntity() {
        RestAssured.when()
                .get("/reactiveRemoveTransientEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveRemoveManagedEntity() {
        RestAssured.when()
                .get("/reactiveRemoveManagedEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveUpdate() {
        RestAssured.when()
                .get("/reactiveUpdate")
                .then()
                .body(is("Tina"));
    }
}

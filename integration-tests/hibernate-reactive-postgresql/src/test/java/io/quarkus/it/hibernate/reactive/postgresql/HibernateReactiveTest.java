package io.quarkus.it.hibernate.reactive.postgresql;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 *
 * Also makes sure that these work with a blocking security implementation
 */
@QuarkusTest
@TestHTTPEndpoint(HibernateReactiveTestEndpoint.class)
public class HibernateReactiveTest {

    @Test
    public void reactiveCowPersist() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactiveCowPersist")
                .then()
                .body(containsString("\"name\":\"Carolina\"}")); //Use containsString as we don't know the Id this object will have
    }

    @Test
    public void reactiveFindMutiny() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactiveFindMutiny")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

    @Test
    public void reactivePersist() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactivePersist")
                .then()
                .body(is("Tulip"));
    }

    @Test
    public void reactiveRemoveTransientEntity() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactiveRemoveTransientEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveRemoveManagedEntity() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactiveRemoveManagedEntity")
                .then()
                .body(is("OK"));
    }

    @Test
    public void reactiveUpdate() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/reactiveUpdate")
                .then()
                .body(is("Tina"));
    }
}

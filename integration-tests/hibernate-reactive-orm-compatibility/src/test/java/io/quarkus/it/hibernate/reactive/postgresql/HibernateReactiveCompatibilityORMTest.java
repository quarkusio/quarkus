package io.quarkus.it.hibernate.reactive.postgresql;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 *
 * Also makes sure that these work with a blocking security implementation
 */
@QuarkusTest
public class HibernateReactiveCompatibilityORMTest {

    @Test
    public void reactiveCowPersistWithORM() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/tests/reactiveCowPersist")
                .then()
                .body(containsString("\"name\":\"Carolina Reactive\"}"));

        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/testsORM/blockingCowPersist")
                .then()
                .body(containsString("\"name\":\"Carolina\"}"));
    }
}

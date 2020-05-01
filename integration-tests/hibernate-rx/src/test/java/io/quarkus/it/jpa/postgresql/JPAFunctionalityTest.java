package io.quarkus.it.jpa.postgresql;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void reactiveFind1() {
        RestAssured.when()
                .get("/tests/reactiveFind1")
                .then()
                .body(is("{\"id\":5,\"name\":\"Aloi\"}"));
    }

}

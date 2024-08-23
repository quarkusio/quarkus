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
    public void base() {
        RestAssured.when().get("/jpa/testfunctionality/base").then().body(is("OK"));
    }

    @Test
    public void uuid() {
        RestAssured.when().get("/jpa/testfunctionality/uuid").then().body(is("OK"));
    }

    @Test
    public void json() {
        RestAssured.when().get("/jpa/testfunctionality/json").then().body(is("OK"));
    }

}

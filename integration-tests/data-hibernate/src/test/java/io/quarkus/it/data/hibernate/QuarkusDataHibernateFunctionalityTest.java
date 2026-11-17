package io.quarkus.it.data.hibernate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusDataHibernateFunctionalityTest {

    @Test
    public void testPersonEndpoint() {
        given()
                .when().get("/persons/test")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testBookManagedEndpoint() {
        given()
                .when().get("/books/test-managed")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testBookStatelessEndpoint() {
        given()
                .when().get("/books/test-stateless")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testCatRestrictionsEndpoint() {
        given()
                .when().get("/cats/test-restrictions")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testActualRestrictionsEndpoint() {
        given()
                .when().get("/restrictions-test/actual-restrictions")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testComprehensiveRestrictionsEndpoint() {
        given()
                .when().get("/comprehensive-restrictions/test-all")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testEdgeCaseRestrictionsEndpoint() {
        given()
                .when().get("/edge-case-restrictions/test-edge-cases")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}

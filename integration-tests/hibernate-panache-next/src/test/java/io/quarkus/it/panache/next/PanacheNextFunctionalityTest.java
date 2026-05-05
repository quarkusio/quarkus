package io.quarkus.it.panache.next;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PanacheNextFunctionalityTest {

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
}

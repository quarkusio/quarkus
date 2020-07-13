package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CartResourceTest {

    @Test
    void testFindAll() {
        when().get("/cart").then()
                .statusCode(200)
                .body(containsString("Jason"))
                .body(containsString("Bourne"));
    }

    @Test
    void testFindAllActiveCarts() {
        when().get("/cart/active").then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void testGetActiveCartForCustomer() {
        when().get("/cart/customer/1").then()
                .statusCode(200)
                .body(containsString("Jason"));
    }

    @Test
    void testFindById() {
        when().get("/cart/1").then()
                .statusCode(200)
                .body(containsString("id"))
                .body(containsString("1"))
                .body(containsString("status"))
                .body(containsString("NEW"));

        when().get("/cart/100").then()
                .statusCode(204)
                .body(emptyOrNullString());
    }

    @Test
    void testDelete() {
        when().get("/cart/active").then()
                .statusCode(200)
                .body(containsString("Jason"))
                .body(containsString("Bourne"));

        when().delete("/cart/1").then()
                .statusCode(204);

        when().get("/cart/active").then()
                .statusCode(200)
                .body("size()", is(1));
    }
}

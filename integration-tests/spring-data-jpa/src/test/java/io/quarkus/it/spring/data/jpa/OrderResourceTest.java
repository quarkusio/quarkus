package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.Is.is;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OrderResourceTest {

    @Test
    void testAll() {
        when().get("/order").then()
                .statusCode(200)
                .body("size()", Matchers.is(2))
                .body(containsString("jason.bourne@mail.com"))
                .body(containsString("homer.simpson@mail.com"));
    }

    @Test
    void testExistsById() {
        when().get("/order/exists/1").then()
                .statusCode(200)
                .body(is("true"));

        when().get("/order/exists/100").then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void testFindByCustomerId() {
        when().get("/order/customer/1").then()
                .statusCode(200)
                .body(containsString("Jason"))
                .body(containsString("Bourne"));
    }

    @Test
    void testNotFoundAfterDeleted() {
        when().get("/order/exists/2").then()
                .statusCode(200)
                .body(is("true"));

        when().delete("/order/2").then()
                .statusCode(204);

        when().get("/order/exists/2").then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void testNotFoundById() {
        when().get("/order/100").then()
                .statusCode(204)
                .body(emptyOrNullString());
    }

}

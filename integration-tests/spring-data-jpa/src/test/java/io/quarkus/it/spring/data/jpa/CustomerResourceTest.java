package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CustomerResourceTest {

    @Test
    void testAll() {
        when().get("/customer").then()
                .statusCode(200)
                .body("size()", is(3))
                .body(containsString("Jason"))
                .body(containsString("Homer"))
                .body(containsString("Peter"));
    }

    @Test
    void testAllActiveUsers() {
        when().get("/customer/active").then()
                .statusCode(200)
                .body(containsString("Simpson"))
                .body(containsString("Homer"));
    }

    @Test
    void testAllInactiveUsers() {
        when().get("/customer/inactive").then()
                .statusCode(200)
                .body(containsString("Peter"))
                .body(containsString("Quin"));
    }

    @Test
    void testFindById() {
        when().get("/customer/1").then()
                .statusCode(200)
                .body(containsString("Jason"))
                .body(containsString("Bourne"));
    }

    @Test
    void testDeleteThenCustomerIsDisabled() {
        when().get("/customer/active").then()
                .statusCode(200)
                .body("size()", is(2));

        when().delete("/customer/1").then()
                .statusCode(204);

        when().get("/customer/active").then()
                .statusCode(200)
                .body("size()", is(1));

        when().get("/customer/inactive").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Jason"))
                .body(containsString("Bourne"));
    }
}

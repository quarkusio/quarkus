package io.quarkus.narayana.jta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionalResourceIT {

    @Test
    void testGet() {
        given().when().get("/hello").then().body(is("0"));
    }
}

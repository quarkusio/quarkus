package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AdminClientTestCase {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/admin-client")
                .then()
                .statusCode(200)
                .body(is("quarkus"));
    }

}
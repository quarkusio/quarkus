package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testHelloEndpoint() {
        when().get("/hello")
                .then()
                .statusCode(401);
    }
}

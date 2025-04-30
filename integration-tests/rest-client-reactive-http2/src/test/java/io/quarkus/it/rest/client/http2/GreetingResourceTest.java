package io.quarkus.it.rest.client.http2;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testGreeting() {
        when()
                .get("/greet")
                .then()
                .statusCode(200);
    }
}

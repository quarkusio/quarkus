package io.quarkus.nats.server.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class NatsServerResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/nats-server")
                .then()
                .statusCode(200)
                .body(is("Hello nats-server"));
    }
}

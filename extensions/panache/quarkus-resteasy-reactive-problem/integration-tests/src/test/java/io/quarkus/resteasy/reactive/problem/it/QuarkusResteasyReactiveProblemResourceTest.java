package io.quarkus.resteasy.reactive.problem.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusResteasyReactiveProblemResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/quarkus-resteasy-reactive-problem")
                .then()
                .statusCode(200)
                .body(is("Hello quarkus-resteasy-reactive-problem"));
    }
}

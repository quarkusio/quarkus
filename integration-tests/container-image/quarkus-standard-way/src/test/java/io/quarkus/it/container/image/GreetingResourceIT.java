package io.quarkus.it.container.image;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@Disabled("This test seems flaky")
@QuarkusIntegrationTest
public class GreetingResourceIT {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }
}

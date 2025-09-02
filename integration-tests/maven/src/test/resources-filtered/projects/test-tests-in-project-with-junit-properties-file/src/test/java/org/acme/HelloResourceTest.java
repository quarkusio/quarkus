package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when()
                .get("/app/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST via config"));
    }

}

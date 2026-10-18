package org.acme.tck;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

// No QuarkusTest annotation, it's added by a service loader
public class HelloResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/app/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

}

package org.acme.qute;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testEndpoint() {
        given()
                .when().get("/qute/hello")
                .then()
                .statusCode(200)
                .body(containsString("<p>Hello world!</p>"));

        given()
                .when().get("/qute/hello?name=Lucie")
                .then()
                .statusCode(200)
                .body(containsString("<p>Hello Lucie!</p>"));
    }

}

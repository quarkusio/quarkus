package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

@MyQuarkusTest
public class HelloResourceMetaAnnotationTest {

    @Test
    public void testHelloEndpointWithMetaAnnotation() {
        given()
                .when().get("/app/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

}

package org.acme

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is

@QuarkusTest
class HelloResourceTest {

    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/app/hello")
                .then()
                .statusCode(200)
                .body(is("hello"))
    }

}

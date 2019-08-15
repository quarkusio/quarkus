package io.quarkus.it.resteasy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ContextResourceTest {

    @Test
    void testEndpoint() {
        given()
                .when().get("/context/servletcontext")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}

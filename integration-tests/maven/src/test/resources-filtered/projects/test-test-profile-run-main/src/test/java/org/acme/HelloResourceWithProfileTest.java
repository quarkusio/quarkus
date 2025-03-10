package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@TestProfile(SomeProfile.class)
@QuarkusTest
public class HelloResourceWithProfileTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/app/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Test
    public void testConfigIsOverridden() {
        given()
                .when().get("/app/config/test.overridden.value")
                .then()
                .statusCode(200)
                .body(is("sausages"));
    }

}

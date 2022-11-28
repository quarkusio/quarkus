package io.quarkus.it.keycloak;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OpenApiOidcApplicationEndpointsTest {

    @Test
    public void testOidcApplicationEndpointsAreListed() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get("/q/openapi")
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey("/code-flow-form-post/front-channel-logout"))
                .body("paths", Matchers.hasKey("/back-channel-logout"))
                .body("paths", Matchers.hasKey("/code-flow/logout"));
    }
}

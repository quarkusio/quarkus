package io.quarkus.it.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * tests that application.properties is read from src/main/resources when running native image tests
 * 
 * This does not necessarily belong here, but main and test-extension have a lot of existing
 * config that would need to be duplicated, so it is here out of convenience.
 */
@QuarkusIntegrationTest
class ApplicationPropertiesOverrideIT {

    @Test
    void testEndpoint() {
        given()
                .when().get("/message")
                .then()
                .statusCode(200)
                .body(containsString("Production"));
    }

}

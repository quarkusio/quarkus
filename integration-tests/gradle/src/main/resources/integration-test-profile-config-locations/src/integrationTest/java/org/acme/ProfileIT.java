package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class ProfileIT {

    @Test
    public void testIntegrationTestProfile() {
        given().when().get("/profile").then().body(startsWith("integration"));
    }
}

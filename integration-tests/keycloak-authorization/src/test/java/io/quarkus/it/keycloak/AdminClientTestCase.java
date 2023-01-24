package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AdminClientTestCase {

    @Test
    public void testGetExistingRealm() {
        when().get("/admin-client/realm")
                .then()
                .statusCode(200)
                .body(equalTo("quarkus"));
    }

    @Test
    public void testGetNewRealm() {
        when().get("/admin-client/newrealm")
                .then()
                .statusCode(200)
                .body(equalTo("quarkus2"));
    }
}

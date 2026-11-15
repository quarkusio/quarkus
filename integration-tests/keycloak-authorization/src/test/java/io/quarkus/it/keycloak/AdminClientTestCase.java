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

    @Test
    public void testRealmClientProfiles() {
        // the realm representation is served by the Jackson 2 Keycloak representations in this application,
        // exercises the Jackson 2 JsonNode bridge of the Jackson 3 based admin client
        when().put("/admin-client/empty-realm-client-profiles")
                .then()
                .statusCode(200)
                .body(equalTo("quarkus:0:true"));
    }
}

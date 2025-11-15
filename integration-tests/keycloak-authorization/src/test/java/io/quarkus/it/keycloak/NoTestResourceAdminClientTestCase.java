package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/*
 * Exercises behaviour in the case where there isn't a test resource to make
 * all the realms and roles.
 */
@TestProfile(NoTestResourceProfile.class)
@QuarkusTest
public class NoTestResourceAdminClientTestCase {

    @Test
    public void testGetRoles() {
        when().get("/admin-client/roles")
                .then()
                .statusCode(200)
                .body(containsString("scratcher-sniffer"));
    }

    @Test
    public void testGetUsers() {
        when().get("/admin-client/users")
                .then()
                .statusCode(200)
                .body(containsString("leia-luke"));
    }

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

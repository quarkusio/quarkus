package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AdminClientTestCase {

    @Test
    public void testGetExistingRealm() {
        RealmRepresentation realm = given()
                .when().get("/admin-client/realm").as(RealmRepresentation.class);
        assertEquals("quarkus", realm.getRealm());
    }

    @Test
    public void testGetNewRealm() {
        RealmRepresentation realm = given()
                .when().get("/admin-client/newrealm").as(RealmRepresentation.class);
        assertEquals("quarkus2", realm.getRealm());
    }
}

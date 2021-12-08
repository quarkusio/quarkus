package org.acme.security.openid.connect;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BearerTokenAuthenticationTest {

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void testAdminAccess() {
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(200);

        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/admin")
                .then()
                .statusCode(403);
    }

    @Test
    public void testUserAccess() {

        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);

        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);
    }

    protected String getAccessToken(String userName) {
        return keycloakClient.getAccessToken(userName);
    }
}

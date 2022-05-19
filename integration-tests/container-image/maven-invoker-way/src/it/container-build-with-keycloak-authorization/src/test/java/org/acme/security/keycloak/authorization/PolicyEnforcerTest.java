package org.acme.security.keycloak.authorization;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class PolicyEnforcerTest {
    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void testAccessUserResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/admin")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/admin")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(200);
    }

    @Test
    public void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public")
                .then()
                .statusCode(204);
    }

    private String getAccessToken(String userName) {
        return keycloakClient.getAccessToken(userName);
    }
}

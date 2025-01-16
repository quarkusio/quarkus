package io.quarkus.it.oidc.dev.services;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class BearerAuthenticationOidcDevServicesTest {

    static final OidcTestClient oidcTestClient = new OidcTestClient();

    @AfterAll
    public static void close() {
        oidcTestClient.close();
    }

    @Test
    public void testLoginAsCustomUser() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald"))
                .get("/secured/admin-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Ronald"))
                .body(Matchers.containsString("admin"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald"))
                .get("/secured/user-only")
                .then()
                .statusCode(403);
    }

    @Test
    public void testLoginAsAlice() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("alice"))
                .get("/secured/admin-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"))
                .body(Matchers.containsString("admin"))
                .body(Matchers.containsString("user"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("alice"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"))
                .body(Matchers.containsString("admin"))
                .body(Matchers.containsString("user"));
    }

    @Test
    public void testLoginAsBob() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/secured/admin-only")
                .then()
                .statusCode(403);
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("bob"))
                .body(Matchers.containsString("user"));
    }

    private String getAccessToken(String user) {
        return oidcTestClient.getAccessToken(user, user);
    }
}

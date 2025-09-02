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
                .body(Matchers.startsWith("alice@example.com "))
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
                .body(Matchers.startsWith("bob@example.com "))
                .body(Matchers.containsString("user"));
    }

    @Test
    void testEmailAndName() {
        // test users get an @example.com appended if username is not an email address
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("bob@example.com "))
                .body(Matchers.containsString(" Bob"));

        // Test no additional @example.com is appended if requested username is likely already an email address
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob@example.com"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("bob@example.com "))
                .body(Matchers.containsString(" Bob"));
    }

    private String getAccessToken(String user) {
        return oidcTestClient.getAccessToken(user, user);
    }
}

package io.quarkus.it.oidc.dev.services;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BearerAuthenticationOidcDevServicesTest {

    @Test
    public void testLoginAsCustomUser() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald", "admin"))
                .get("/secured/admin-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Ronald"))
                .body(Matchers.containsString("admin"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald", "admin"))
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
        return RestAssured.given().get(getAuthServerUrl() + "/testing/generate/access-token?user=" + user).asString();
    }

    private String getAccessToken(String user, String... roles) {
        return RestAssured.given()
                .get(getAuthServerUrl() + "/testing/generate/access-token?user=" + user + "&roles=" + String.join(",", roles))
                .asString();
    }

    private static String getAuthServerUrl() {
        return RestAssured.get("/secured/auth-server-url").then().statusCode(200).extract().body().asString();
    }
}

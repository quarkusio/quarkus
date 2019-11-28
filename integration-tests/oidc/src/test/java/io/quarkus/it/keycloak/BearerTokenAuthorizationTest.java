package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class BearerTokenAuthorizationTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @Test
    public void testSecureAccessSuccessWithCors() {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET";
        String headers = "X-Custom";
        RestAssured.given().header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .options("/api").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers);

        for (String username : Arrays.asList("alice", "jdoe", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username))
                    .when().get("/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testSecureAccessSuccessCustomPrincipal() {
        for (String username : Arrays.asList("alice", "jdoe", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username))
                    .when().get("/api/users/me")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username + "@gmail.com"));
        }
    }

    @Test
    public void testSecureAccessSuccessPreferredUsername() {
        for (String username : Arrays.asList("alice", "jdoe", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username))
                    .when().get("/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("granted:admin"));
    }

    @Test
    public void testPermissionHttpInformationProvider() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/http-cip")
                .then()
                .statusCode(200)
                .body("preferred_username", equalTo("alice"));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/admin")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDeniedNoBearerToken() {
        RestAssured.given()
                .when().get("/api/users/me").then()
                .statusCode(401);
    }

    private String getAccessToken(String userName) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }
}

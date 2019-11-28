package io.quarkus.it.keycloak;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class PolicyEnforcerTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
    }

    @AfterAll
    public static void removeKeycloakRealm() {
    }

    @Test
    public void testUserHasRoleConfidential() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource"));
        ;
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
    }

    @Test
    public void testRequestParameterAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=true")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Claim Protected Resource"));
        ;
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=false")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testHttpResponseFromExternalServiceAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Http Response Claim Protected Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testBodyClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .contentType(ContentType.JSON)
                .body("{\"from-body\": \"grant\"}")
                .when()
                .post("/api/permission/body-claim")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Body Claim Protected Resource"));
    }

    @Test
    public void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public")
                .then()
                .statusCode(204);
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

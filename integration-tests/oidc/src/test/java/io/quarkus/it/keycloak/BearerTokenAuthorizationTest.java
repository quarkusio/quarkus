package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.KeycloakRealmResourceManager.getAccessToken;
import static io.quarkus.it.keycloak.KeycloakRealmResourceManager.getRefreshToken;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
@Disabled("Vert.x 4 Integration in progress - https://github.com/quarkusio/quarkus/issues/15084")
public class BearerTokenAuthorizationTest {

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
    public void testBasicAuth() {
        byte[] basicAuthBytes = "alice:password".getBytes(StandardCharsets.UTF_8);
        RestAssured.given()
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuthBytes))
                .when().get("/api/users/me")
                .then()
                .statusCode(200)
                .body("userName", equalTo("alice"));
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
    public void testAccessAdminResourceCustomHeaderNoBearerScheme() {
        RestAssured.given().header("X-Forwarded-Authorization", getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(401);
    }

    @Test
    public void testAccessAdminResourceCustomHeaderBearerScheme() {
        RestAssured.given().header("X-Forwarded-Authorization", getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(401);
    }

    @Test
    public void testAccessAdminResourceWithRefreshToken() {
        RestAssured.given().auth().oauth2(getRefreshToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(401);
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

    //see https://github.com/quarkusio/quarkus/issues/5809
    @RepeatedTest(20)
    public void testOidcAndVertxHandler() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().body("Hello World").post("/vertx")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World"));
    }

    @Test
    public void testExpiredBearerToken() throws InterruptedException {
        String token = getAccessToken("alice");

        await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS).until(
                        () -> RestAssured.given().auth().oauth2(token).when()
                                .get("/api/users/me").thenReturn().statusCode() == 401);
    }
}

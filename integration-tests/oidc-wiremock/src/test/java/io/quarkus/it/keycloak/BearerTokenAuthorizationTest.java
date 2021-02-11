package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
public class BearerTokenAuthorizationTest {

    @Test
    public void testSecureAccessSuccessPreferredUsername() {
        for (String username : Arrays.asList("alice", "jdoe", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username, new HashSet<>(Arrays.asList("user", "admin"))))
                    .when().get("/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("admin", new HashSet<>(Arrays.asList("admin"))))
                .when().get("/api/admin")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user"))))
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

    @Test
    public void testExpiredBearerToken() {
        String token = getExpiredAccessToken("alice", new HashSet<>(Arrays.asList("user")));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me")
                .then()
                .statusCode(401);
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .jws()
                .keyId("1")
                .sign();
    }

    private String getExpiredAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .expiresAt(Instant.MIN)
                .jws()
                .keyId("1")
                .sign();
    }

}

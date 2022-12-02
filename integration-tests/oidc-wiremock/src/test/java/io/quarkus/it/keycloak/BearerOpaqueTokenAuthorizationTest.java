package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class BearerOpaqueTokenAuthorizationTest {

    @Test
    public void testSecureAccessSuccessPreferredUsername() {
        for (String username : Arrays.asList("alice", "admin")) {
            RestAssured.given()
                    .header("Authorization", "Bearer " + username)
                    .when().get("/opaque/api/users/preferredUserName/bearer")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given()
                .header("Authorization", "Bearer " + "admin")
                .when().get("/opaque/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given()
                .header("Authorization", "Bearer " + "alice")
                .when().get("/opaque/api/admin/bearer")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDeniedNoBearerToken() {
        RestAssured.given()
                .when().get("/opaque/api/users/me/bearer").then()
                .statusCode(401);
    }

    @Test
    public void testExpiredBearerToken() {

        RestAssured.given()
                .header("Authorization", "Bearer " + "expired")
                .get("/opaque/api/users/me/bearer")
                .then()
                .statusCode(401);
    }

}

package io.quarkus.it.keycloak;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;

@TestProfile(BearerTokenManagementInterfaceTest.ManagementInterfaceProfile.class)
@QuarkusTest
public class BearerTokenManagementInterfaceTest {

    @TestHTTPResource(value = "/management-secured", management = true)
    URL managementSecured;

    @TestHTTPResource(value = "/management-public", management = true)
    URL managementPublic;

    @Test
    public void testPublicManagementRoute() {
        // anonymous request to a public route -> success
        RestAssured.given()
                .when().get(managementPublic)
                .then()
                .statusCode(200)
                .body(Matchers.is("this route is public"));
        // route is public, but proactive auth is enabled, credentials are sent and RS256 is rejected
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.RS256, "admin"))
                .when().get(managementPublic)
                .then()
                .statusCode(401);
        // PS256 is OK, 'management' roles is missing but no roles are required -> success
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.PS256, "admin"))
                .when().get(managementPublic)
                .then()
                .statusCode(200)
                .body(Matchers.is("this route is public"));
    }

    @Test
    public void testManagementRouteSecuredWithHttpPerm() {
        // RS256 is rejected
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.RS256, "admin"))
                .when().get(managementSecured)
                .then()
                .statusCode(401);
        // PS256 is OK but 'management' roles is missing
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.PS256, "admin"))
                .when().get(managementSecured)
                .then()
                .statusCode(403);
        // PS256 is OK but 'management' roles is missing
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.PS256, "management"))
                .when().get(managementSecured)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testMainRouterAuthenticationWorks() {
        // RS256 is rejected
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.RS256, "admin"))
                .when().get("/api/admin/bearer-required-algorithm")
                .then()
                .statusCode(401);
        // PS256 is OK
        RestAssured.given().auth().oauth2(getAccessToken("admin", SignatureAlgorithm.PS256, "admin"))
                .when().get("/api/admin/bearer-required-algorithm")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    private static String getAccessToken(String userName, SignatureAlgorithm alg, String... roles) {
        return Jwt.preferredUserName(userName)
                .groups(Set.copyOf(Arrays.asList(roles)))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().algorithm(alg)
                .sign();
    }

    public static class ManagementInterfaceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.management.enabled", "true",
                    "quarkus.management.auth.enabled", "true",
                    "quarkus.oidc.bearer-required-algorithm.tenant-paths", "*",
                    "quarkus.management.auth.permission.custom.paths", "/q/management-secured",
                    "quarkus.management.auth.permission.custom.policy", "management-policy",
                    "quarkus.management.auth.policy.management-policy.roles-allowed", "management");
        }
    }
}

package io.quarkus.keycloak.pep.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.keycloak.pep.runtime.PolicyEnforcerResolver;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class KeycloakAuthorizationOfflineEnforcerTest {

    @RegisterExtension
    final static QuarkusUnitTest app = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AdminResource.class, PublicResource.class, UserResource.class)
                    .addAsResource("offline-enforcer-application.properties", "application.properties")
                    .addAsResource("quarkus-realm.json"));

    @Inject
    PolicyEnforcerResolver resolver;

    @Inject
    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @Test
    public void testNoPathMatcherRemoteCalls() {

        // first test Keycloak policy enforcer works
        testPublicResource();
        testAccessUserResource();
        testAccessAdminResource();

        // as we can't test whether there were remote calls without extensive logging
        // here we test whether expected decisions are made
        testIsPathEnforcementDisabled();
    }

    @Test
    public void testConflictingPaths() {
        // path '/api/conflict/*' is both enforced and disabled
        // therefore we expect path to be disabled to stay on the safe side
        var defaultEnforcer = resolver.getPolicyEnforcer(null);
        assertTrue(defaultEnforcer.isPathEnforcementDisabled("/api/conflict/stuff"));
    }

    private void testIsPathEnforcementDisabled() {
        var defaultEnforcer = resolver.getPolicyEnforcer(null);
        assertTrue(defaultEnforcer.isPathEnforcementDisabled("/api/public/serve"));
        assertTrue(defaultEnforcer.isPathEnforcementDisabled("/api/public"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/users"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/users/me"));
    }

    private void testAccessUserResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/users/me")
                .then()
                .statusCode(200);
    }

    private void testAccessAdminResource() {
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

    private void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public/serve")
                .then()
                .statusCode(200);
    }

    private String getAccessToken(String userName) {
        return RestAssured
                .given()
                .relaxedHTTPSValidation()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "backend-service")
                .param("client_secret", "secret")
                .when()
                .post(keycloakUrl + "/realms/quarkus/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }
}

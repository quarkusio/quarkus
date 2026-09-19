package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;

/**
 * Verifies the {@code @AuthenticationContext} step-up policy is enforced for {@code @TestSecurity}
 * tests, with the required 'acr' and 'auth_time' claims supplied via {@code @OidcSecurity}.
 */
@TestProfile(BearerTokenStepUpAuthenticationTest.StepUpAuthTestProfile.class)
@QuarkusTestResource(KeycloakRealmResourceManager.class)
@QuarkusTest
public class StepUpAuthenticationTestSecurityTest {

    @Test
    @TestSecurity(user = "alice")
    public void testRequiredAcrClaimMissing() {
        RestAssured.given()
                .when().get("/step-up-auth/method-level/no-rbac-annotation")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "acr", value = "5"))
    public void testWrongAcrClaim() {
        RestAssured.given()
                .when().get("/step-up-auth/method-level/no-rbac-annotation")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "acr", value = "1"))
    public void testMatchingAcrClaim() {
        RestAssured.given()
                .when().get("/step-up-auth/method-level/no-rbac-annotation")
                .then().statusCode(200)
                .body(is("no-rbac-annotation"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "acr", value = "myACR"))
    public void testMaxAgeWithFreshToken() {
        // no 'auth_time' claim is defined, so the 'iat' claim of the generated token is used
        RestAssured.given()
                .when().get("/step-up-auth/method-level/max-age-and-acr-required")
                .then().statusCode(200)
                .body(is("max-age-and-acr-required"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = { @Claim(key = "acr", value = "myACR"), @Claim(key = "auth_time", value = "123") })
    public void testMaxAgeExceeded() {
        RestAssured.given()
                .when().get("/step-up-auth/method-level/max-age-and-acr-required")
                .then().statusCode(401);
    }
}

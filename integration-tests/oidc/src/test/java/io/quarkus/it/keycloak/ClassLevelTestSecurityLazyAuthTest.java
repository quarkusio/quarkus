package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;

@TestSecurity(user = "user1", roles = "viewer")
@QuarkusTest
@TestHTTPEndpoint(ProtectedJwtResource.class)
public class ClassLevelTestSecurityLazyAuthTest {

    @Test
    public void testClassAnnotationWithDummyUser() {
        RestAssured.when().get("test-security").then().body(is("user1"));
    }

    @Test
    @OidcSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testClassAnnotationWithOidcSecurity() {
        // verify information form security context
        RestAssured.when().get("test-security").then().body(is("user1"));
        // verify information from JWT
        RestAssured.when().get("test-security-jwt").then().body(is("user1:viewer:user@gmail.com"));
    }

    @Test
    @TestSecurity(user = "userJwt", roles = "viewer")
    @OidcSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testMethodLevelTestSecurityOverridesClassAnnotation() {
        RestAssured.when().get("test-security-jwt").then().body(is("userJwt:viewer:user@gmail.com"));
    }

}

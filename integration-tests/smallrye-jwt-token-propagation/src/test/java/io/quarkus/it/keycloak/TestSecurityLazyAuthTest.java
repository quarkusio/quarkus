package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(ProtectedJwtResource.class)
public class TestSecurityLazyAuthTest {

    @Test
    public void testTestSecurityAnnotationWithAugmentors_anonymousUser() {
        TestSecurityIdentityAugmentor.resetInvoked();
        // user is not authenticated and doesn't have required role granted by the augmentor
        RestAssured.get("test-security-with-augmentors").then().statusCode(401);
        // identity manager applies augmentors on anonymous identity
        // because @TestSecurity is not in action and that's what we do for the anonymous requests
        Assertions.assertTrue(TestSecurityIdentityAugmentor.isInvoked());
    }

    @TestSecurity(user = "authenticated-user")
    @Test
    public void testTestSecurityAnnotationNoAugmentors_authenticatedUser() {
        TestSecurityIdentityAugmentor.resetInvoked();
        // user is authenticated, but doesn't have required role granted by the augmentor
        // and no augmentors are applied
        RestAssured.get("test-security-with-augmentors").then().statusCode(403);
        Assertions.assertFalse(TestSecurityIdentityAugmentor.isInvoked());
    }

    @TestSecurity(user = "authenticated-user", augmentors = TestSecurityIdentityAugmentor.class)
    @Test
    public void testTestSecurityAnnotationWithAugmentors_authenticatedUser() {
        TestSecurityIdentityAugmentor.resetInvoked();
        // user is authenticated, but doesn't have required role granted by the augmentor
        RestAssured.get("test-security-with-augmentors").then().statusCode(403);
        Assertions.assertTrue(TestSecurityIdentityAugmentor.isInvoked());
    }

    @TestSecurity(user = "authorized-user")
    @Test
    public void testTestSecurityAnnotationNoAugmentors_authorizedUser() {
        // should fail because no augmentors are applied
        TestSecurityIdentityAugmentor.resetInvoked();
        RestAssured.get("test-security-with-augmentors").then().statusCode(403);
        Assertions.assertFalse(TestSecurityIdentityAugmentor.isInvoked());
    }

    @TestSecurity(user = "authorized-user", augmentors = TestSecurityIdentityAugmentor.class)
    @Test
    public void testTestSecurityAnnotationWithAugmentors_authorizedUser() {
        TestSecurityIdentityAugmentor.resetInvoked();
        RestAssured.get("test-security-with-augmentors").then().statusCode(200)
                .body(is("authorized-user:authorized-user:authorized-user"));
        Assertions.assertTrue(TestSecurityIdentityAugmentor.isInvoked());
    }

    @Test
    @TestAsUser1Viewer
    public void testWithDummyUser() {
        RestAssured.when().get("test-security").then()
                .body(is("user1:user1:user1"));
    }

    @Test
    @TestAsUser1Tester
    public void testWithDummyUserForbidden() {
        RestAssured.when().get("test-security").then().statusCode(403);
    }

    @Test
    @TestAsUser1Viewer
    public void testPostWithDummyUser() {
        RestAssured.given().contentType(ContentType.JSON).when().body("{\"name\":\"user1\"}").post("test-security").then()
                .body(is("user1:user1"));
    }

    @Test
    @TestAsUser1Tester
    public void testPostWithDummyUserForbidden() {
        RestAssured.given().contentType(ContentType.JSON).when().body("{\"name\":\"user1\"}").post("test-security").then()
                .statusCode(403);
    }

    @Test
    @TestAsUserJwtViewer
    public void testJwtGetWithDummyUser() {
        RestAssured.when().get("test-security-jwt").then()
                .body(is("userJwt:userJwt:userJwt:viewer:user@gmail.com"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @TestSecurity(user = "user1", roles = "viewer")
    public @interface TestAsUser1Viewer {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @TestSecurity(user = "user1", roles = "tester")
    public @interface TestAsUser1Tester {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @TestSecurity(user = "userJwt", roles = "viewer")
    @JwtSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public @interface TestAsUserJwtViewer {

    }

}

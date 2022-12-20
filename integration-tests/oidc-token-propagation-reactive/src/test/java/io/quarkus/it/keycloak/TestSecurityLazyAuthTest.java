package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(ProtectedJwtResource.class)
public class TestSecurityLazyAuthTest {

    @Test
    @TestAsUser1Viewer
    public void testWithDummyUser() {
        RestAssured.when().get("test-security").then()
                .body(is("user1"));
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
                .body(is("userJwt:viewer:user@gmail.com"));
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
    @OidcSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public @interface TestAsUserJwtViewer {

    }

}

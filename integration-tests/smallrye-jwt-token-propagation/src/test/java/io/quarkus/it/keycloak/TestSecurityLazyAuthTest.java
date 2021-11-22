package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

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
    @TestSecurity(user = "user1", roles = "viewer")
    public void testWithDummyUser() {
        RestAssured.when().get("test-security").then()
                .body(is("user1"));
    }

    @Test
    @TestSecurity(user = "user1", roles = "tester")
    public void testWithDummyUserForbidden() {
        RestAssured.when().get("test-security").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "user1", roles = "viewer")
    public void testPostWithDummyUser() {
        RestAssured.given().contentType(ContentType.JSON).when().body("{\"name\":\"user1\"}").post("test-security").then()
                .body(is("user1:user1"));
    }

    @Test
    @TestSecurity(user = "user1", roles = "tester")
    public void testPostWithDummyUserForbidden() {
        RestAssured.given().contentType(ContentType.JSON).when().body("{\"name\":\"user1\"}").post("test-security").then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "userJwt", roles = "viewer")
    @JwtSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testJwtGetWithDummyUser() {
        RestAssured.when().get("test-security-jwt").then()
                .body(is("userJwt:viewer:user@gmail.com"));
    }

}

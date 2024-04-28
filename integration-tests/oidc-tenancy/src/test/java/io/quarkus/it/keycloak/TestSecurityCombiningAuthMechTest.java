package io.quarkus.it.keycloak;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@TestHTTPEndpoint(MultipleAuthMechResource.class)
@QuarkusTest
public class TestSecurityCombiningAuthMechTest {

    @TestSecurity(user = "testUser", authMechanism = "basic")
    @Test
    public void testBasicAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/annotation")
                .then()
                .statusCode(401);
    }

    @TestSecurity(user = "testUser", authMechanism = "Bearer")
    @Test
    public void testBearerBasedAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("bearer/policy")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("bearer/annotation")
                .then()
                .statusCode(200);
    }

    @TestSecurity(user = "testUser", authMechanism = "custom")
    @Test
    public void testCustomAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/annotation")
                .then()
                .statusCode(401);
    }
}

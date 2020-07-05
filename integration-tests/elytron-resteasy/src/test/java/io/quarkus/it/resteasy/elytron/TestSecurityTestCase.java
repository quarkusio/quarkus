package io.quarkus.it.resteasy.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class TestSecurityTestCase {

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testGet() {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(200)
                .body(is("secure"));
    }

    @Test
    @TestSecurity
    void testGetWithSecEnabled() {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "wrong")
    void testGetWithTestUser() {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "wrong")
    void testGetWithTestUserwrongRole() {
        given()
                .when()
                .get("/user")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user")
    void testTestUserCorrectRole() {
        given()
                .when()
                .get("/user")
                .then()
                .statusCode(200)
                .body(is("testUser"));
    }

}

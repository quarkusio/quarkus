package io.quarkus.elytron.security.ldap.it;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class ElytronSecurityLdapTest {

    @Test
    void anonymous() {
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    void standard_role_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(401);
    }

    @Test
    void standard_role_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(200);
    }

    @Test
    void standard_role_not_authorized() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("adminUser", "adminUserPassword")
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(403);
    }

    @Test
    void admin_role_authorized() {
        RestAssured.given()
                .when()
                .auth().preemptive().basic("adminUser", "adminUserPassword")
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(200);
    }

    @Test
    void admin_role_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(401);
    }

    @Test
    void admin_role_not_authorized() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(403);
    }
}

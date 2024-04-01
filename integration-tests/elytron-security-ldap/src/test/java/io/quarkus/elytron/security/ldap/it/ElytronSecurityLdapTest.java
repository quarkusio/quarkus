package io.quarkus.elytron.security.ldap.it;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPException;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class ElytronSecurityLdapTest {

    InMemoryDirectoryServer ldapServer;

    @Test
    @Order(1)
    void anonymous() {
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    @Order(2)
    void standard_role_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(3)
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
    @Order(4)
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
    @Order(5)
    void admin_role_authorized() {
        RestAssured.given()
                .when()
                .auth().preemptive().basic("adminUser", "adminUserPassword")
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    void admin_role_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    void admin_role_not_authorized() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .get("/api/requiresAdminRole")
                .then()
                .statusCode(403);
    }

    @Test()
    @Order(8)
    void standard_role_authenticated_cached() throws LDAPException {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(200);

        ldapServer.shutDown(false);

        RestAssured.given()
                .redirects().follow(false)
                .when()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(200);
    }

}

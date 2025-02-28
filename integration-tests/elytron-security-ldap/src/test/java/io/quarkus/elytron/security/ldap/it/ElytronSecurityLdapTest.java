package io.quarkus.elytron.security.ldap.it;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.ldap.LdapServerTestResource;
import io.restassured.RestAssured;

@WithTestResource(LdapServerTestResource.class)
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

    @Test
    @Order(8)
    void testMappingOfLdapGroupsToIdentityRoles() {
        // LDAP groups are added as SecurityIdentity roles
        // according to the quarkus.security.ldap.identity-mapping.attribute-mappings
        // this test verifies that LDAP groups can be remapped to application-specific SecurityIdentity roles
        // role 'adminRole' comes from 'cn' and we remapped it to 'root'
        RestAssured.given()
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .when()
                .get("/api/requiresRootRole")
                .then()
                .statusCode(403);
        RestAssured.given()
                .auth().preemptive().basic("adminUser", "adminUserPassword")
                .when()
                .get("/api/requiresRootRole")
                .then()
                .statusCode(200)
                .body(containsString("adminUser")); // that is uid
    }

    @Test
    @Order(9)
    void standard_role_authenticated_cached() {
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

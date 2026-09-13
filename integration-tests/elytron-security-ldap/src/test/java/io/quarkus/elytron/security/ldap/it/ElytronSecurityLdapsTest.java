package io.quarkus.elytron.security.ldap.it;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Authenticates against an LDAPS server trusted through a named TLS configuration, which in native mode exercises the
 * reflective loading of the socket factory by JNDI.
 */
@WithTestResource(LdapsServerTestResource.class)
@QuarkusTest
class ElytronSecurityLdapsTest {

    @Test
    void standardRoleAuthenticatedOverLdaps() {
        RestAssured.given()
                .redirects().follow(false)
                .auth().preemptive().basic("standardUser", "standardUserPassword")
                .when()
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(200);
    }

    @Test
    void standardRoleNotAuthorizedOverLdaps() {
        RestAssured.given()
                .redirects().follow(false)
                .auth().preemptive().basic("adminUser", "adminUserPassword")
                .when()
                .get("/api/requiresStandardRole")
                .then()
                .statusCode(403);
    }
}

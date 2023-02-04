package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@ApplicationScoped //see https://github.com/quarkusio/quarkus/issues/11465
public class TestSecurityTest {

    @Test
    public void testSecuredWithDisabledAuth() {
        RestAssured.when().get("/api/securedMethod").then()
                .body(is("accessibleForAdminOnly"));
    }

    @Test
    public void testPreAuthorizeWithDisabledAuth() {
        RestAssured.when().get("/api/allowedForUserOrViewer").then()
                .body(is("allowedForUserOrViewer"));
    }

    @Test
    @TestSecurity(user = "dummy", roles = "viewer")
    public void testWithTestSecurityAndWrongRole() {
        RestAssured.when().get("/api/securedMethod").then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dummy", roles = "admin")
    public void testWithTestSecurityAndCorrectRole() {
        RestAssured.when().get("/api/securedMethod").then()
                .body(is("accessibleForAdminOnly"));
    }
}

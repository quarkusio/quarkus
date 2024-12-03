package io.quarkus.elytron.security.ldap;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.elytron.security.ldap.rest.ParametrizedPathsResource;
import io.quarkus.elytron.security.ldap.rest.RolesEndpointClassLevel;
import io.quarkus.elytron.security.ldap.rest.SingleRoleSecuredServlet;
import io.quarkus.elytron.security.ldap.rest.SubjectExposingResource;
import io.quarkus.elytron.security.ldap.rest.TestApplication;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.ldap.LdapServerTestResource;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * Tests of BASIC authentication mechanism with the minimal config required
 */

@QuarkusTestResource(LdapServerTestResource.class)
public abstract class LdapSecurityRealmTest {

    protected static Class[] testClasses = {
            SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
            ParametrizedPathsResource.class, SubjectExposingResource.class
    };

    // Basic @ServletSecurity tests
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.given().redirects().follow(false).get("/servlet-secured").then()
                .statusCode(getAuthFailureStatusCode());
    }

    protected int getAuthFailureStatusCode() {
        return 401;
    }

    @Test()
    public void testNotSearchingRecursiveFailure() {
        setupAuth("subUser", "subUserPassword")
                .when().redirects().follow(false).get("/servlet-secured").then()
                .statusCode(getAuthFailureStatusCode());
    }

    @Test()
    public void testSecureRoleFailure() {
        setupAuth("noRoleUser", "noRoleUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(403);
    }

    @Test()
    public void testSecureAccessSuccess() {
        String username = "standardUser";
        String password = "standardUserPassword";
        RequestSpecification requestSpec = setupAuth(username, password);
        requestSpec.when().get("/servlet-secured").then()
                .statusCode(200);
    }

    protected RequestSpecification setupAuth(String username, String password) {
        return RestAssured.given().auth().preemptive().basic(username, password);
    }

    /**
     * Test access a secured jaxrs resource without any authentication. should see 401 error code.
     */
    @Test
    public void testJaxrsGetFailure() {
        RestAssured.given().redirects().follow(false).get("/jaxrs-secured/roles-class").then()
                .statusCode(getAuthFailureStatusCode());
    }

    /**
     * Test access a secured jaxrs resource with authentication, but no authorization. should see 403 error code.
     */
    @Test
    public void testJaxrsGetRoleFailure() {
        setupAuth("noRoleUser", "noRoleUserPassword")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsGetRoleSuccess() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathAdminRoleSuccess() {
        setupAuth("adminUser", "adminUserPassword")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(200);
    }

    @Test
    public void testJaxrsPathAdminRoleFailure() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathUserRoleSuccess() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/view").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsUserRoleSuccess() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/subject/secured").then()
                .statusCode(200)
                .body(equalTo(expectedStandardUserName()));
    }

    @Test
    public void testJaxrsInjectedPrincipalSuccess() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/subject/principal-secured").then()
                .statusCode(200)
                .body(equalTo(expectedStandardUserName()));
    }

    protected String expectedStandardUserName() {
        return "standardUser";
    }

    /**
     * Test access a @PermitAll secured jaxrs resource without any authentication. should see a 200 success code.
     */
    @Test
    public void testJaxrsGetPermitAll() {
        RestAssured.when().get("/jaxrs-secured/subject/unsecured").then()
                .statusCode(200)
                .body(equalTo("anonymous"));
    }

    /**
     * Test access a @DenyAll secured jaxrs resource without authentication. should see a 401 success code.
     */
    @Test
    public void testJaxrsGetDenyAllWithoutAuth() {
        RestAssured.given().redirects().follow(false).get("/jaxrs-secured/subject/denied").then()
                .statusCode(getAuthFailureStatusCode());
    }

    /**
     * Test access a @DenyAll secured jaxrs resource with authentication. should see a 403 success code.
     */
    @Test
    public void testJaxrsGetDenyAllWithAuth() {
        setupAuth("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/subject/denied").then()
                .statusCode(403);
    }
}

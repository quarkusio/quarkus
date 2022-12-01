package io.quarkus.elytron.security.jdbc;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

/**
 * Tests of BASIC authentication mechanism with the minimal config required
 */
public abstract class JdbcSecurityRealmTest {

    protected static Class[] testClasses = {
            SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
            ParametrizedPathsResource.class, SubjectExposingResource.class
    };

    // Basic @ServletSecurity tests
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/servlet-secured").then()
                .statusCode(401);
    }

    @Test()
    public void testSecureRoleFailure() {
        RestAssured.given().auth().preemptive().basic("noRoleUser", "noRoleUser")
                .when().get("/servlet-secured").then()
                .statusCode(403);
    }

    @Test()
    public void testSecureAccessSuccess() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource without any authentication. should see 401 error code.
     */
    @Test
    public void testJaxrsGetFailure() {
        RestAssured.when().get("/jaxrs-secured/roles-class").then()
                .statusCode(401);
    }

    /**
     * Test access a secured jaxrs resource with authentication, but no authorization. should see 403 error code.
     */
    @Test
    public void testJaxrsGetRoleFailure() {
        RestAssured.given().auth().preemptive().basic("noRoleUser", "noRoleUser")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsGetRoleSuccess() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathAdminRoleSuccess() {
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(200);
    }

    @Test
    public void testJaxrsPathAdminRoleFailure() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathUserRoleSuccess() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/view").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsUserRoleSuccess() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/subject/secured").then()
                .statusCode(200)
                .body(equalTo("user"));
    }

    @Test
    public void testJaxrsInjectedPrincipalSuccess() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/subject/principal-secured").then()
                .statusCode(200)
                .body(equalTo("user"));
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
        RestAssured.when().get("/jaxrs-secured/subject/denied").then()
                .statusCode(401);
    }

    /**
     * Test access a @DenyAll secured jaxrs resource with authentication. should see a 403 success code.
     */
    @Test
    public void testJaxrsGetDenyAllWithAuth() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/jaxrs-secured/subject/denied").then()
                .statusCode(403);
    }
}

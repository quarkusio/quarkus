package io.quarkus.undertow.test;

import static io.quarkus.undertow.test.ProgrammaticLoginServlet.PASSWORD;
import static io.quarkus.undertow.test.ProgrammaticLoginServlet.USER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

/**
 * {@code HttpServletRequest#login(String, String)} must create the form authentication credential cookie, so that the
 * user stays authenticated on subsequent requests, and {@code HttpServletRequest#logout()} must clear it.
 *
 * @see <a href="https://github.com/quarkusio/quarkus-http/issues/169">quarkus-http#169</a>
 */
public class ProgrammaticLoginFormAuthCookieTestCase {

    private static final String COOKIE_NAME = "quarkus-credential";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProgrammaticLoginServlet.class, TestIdentityProvider.class,
                            TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("""
                            quarkus.http.auth.form.enabled=true
                            quarkus.http.auth.form.login-page=login
                            quarkus.http.auth.form.error-page=error
                            quarkus.http.auth.form.landing-page=landing
                            quarkus.http.auth.permission.secured.paths=/secured/*
                            quarkus.http.auth.permission.secured.policy=authenticated
                            """), "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testProgrammaticLoginCreatesFormAuthCookie() {
        // not authenticated: form auth redirects to the login page
        RestAssured.given().redirects().follow(false).get("/secured/user").then().statusCode(302);

        // wrong password: no cookie
        Response failed = RestAssured.given().queryParams(Map.of(USER, "admin", PASSWORD, "wrong"))
                .get("/programmatic/login");
        failed.then().statusCode(401);
        assertNull(failed.getDetailedCookie(COOKIE_NAME));

        Response login = RestAssured.given().queryParams(Map.of(USER, "admin", PASSWORD, "admin"))
                .get("/programmatic/login");
        login.then().statusCode(200).body(equalTo("admin"));
        Cookie credential = login.getDetailedCookie(COOKIE_NAME);
        assertNotNull(credential, "HttpServletRequest#login() did not create the " + COOKIE_NAME + " cookie");
        assertTrue(credential.isHttpOnly());

        // the credential cookie alone (no HTTP session) authenticates the next request
        RestAssured.given().redirects().follow(false).cookie(COOKIE_NAME, credential.getValue())
                .get("/secured/user")
                .then().statusCode(200).body(equalTo("admin"));

        // logout clears the cookie
        Response logout = RestAssured.given().redirects().follow(false).cookie(COOKIE_NAME, credential.getValue())
                .get("/programmatic/logout");
        logout.then().statusCode(200);
        Cookie cleared = logout.getDetailedCookie(COOKIE_NAME);
        assertNotNull(cleared, "HttpServletRequest#logout() did not clear the " + COOKIE_NAME + " cookie");
        assertEquals("", cleared.getValue());
    }
}

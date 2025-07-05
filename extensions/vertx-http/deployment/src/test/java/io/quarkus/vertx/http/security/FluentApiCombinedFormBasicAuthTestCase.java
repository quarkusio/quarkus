package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class FluentApiCombinedFormBasicAuthTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    PathHandler.class, HttpSecurityConfigurator.class));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testFormBasedAuthSuccess() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .cookie("quarkus-redirect-location",
                        detailedCookie().sameSite("Strict").path(equalTo("/")).value(containsString("/admin")));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/admin"))
                .cookie("quarkus-credential", detailedCookie().sameSite("Strict").path(equalTo("/")));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/admin"));

    }

    @Test
    public void testPathSpecificFormBasedAuthSuccess() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/other-login")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/custom-login"))
                .cookie("quarkus-redirect-location",
                        detailedCookie().sameSite("Strict").path(equalTo("/")).value(containsString("/other-login")));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/other-login"))
                .cookie("quarkus-credential", detailedCookie().sameSite("Strict").path(equalTo("/")));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/other-login")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/other-login"));

    }

    @Test
    public void testFormBasedAuthSuccessLandingPage() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/landing"))
                .cookie("quarkus-credential", notNullValue());

    }

    @Test
    public void testFormAuthFailure() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "wrongpassword")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/error"));

    }

    @Test
    public void testBasicBasedAuthSuccess() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/admin"));
    }

    @Test
    public void testBasicAuthFailure() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "wrongpassword")
                .redirects().follow(false)
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("basic realm=\"TestRealm\""));
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "wrongpassword")
                .redirects().follow(false)
                .get("/other-realm")
                .then()
                .assertThat()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("basic realm=\"OtherTestRealm\""));
    }

    public static class HttpSecurityConfigurator {

        void configureHttpPermissions(@Observes HttpSecurity httpSecurity) {
            var formBuilder = Form.builder().loginPage("login").errorPage("error").landingPage("landing");
            httpSecurity
                    .mechanism(Basic.realm("TestRealm"))
                    .mechanism(formBuilder.build())
                    .path("/admin").roles("admin")
                    .path("/other-realm").basic(Basic.realm("OtherTestRealm")).authenticated()
                    .path("/other-login").form(formBuilder.loginPage("/custom-login").build()).roles("admin");
        }

    }
}

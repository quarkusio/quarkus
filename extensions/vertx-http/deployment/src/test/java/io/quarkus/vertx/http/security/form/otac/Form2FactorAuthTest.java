package io.quarkus.vertx.http.security.form.otac;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.PathHandler;
import io.quarkus.vertx.http.security.TestTrustedIdentityProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.matcher.RestAssuredMatchers;

public class Form2FactorAuthTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    PathHandler.class, InMemoryAuthTokenTestSender.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.authentication-token.enabled=true
                    quarkus.http.auth.form.login-page=login
                    quarkus.http.auth.form.error-page=error
                    quarkus.http.auth.form.landing-page=landing
                    quarkus.http.auth.policy.r1.roles-allowed=admin
                    quarkus.http.auth.permission.roles1.paths=/admin
                    quarkus.http.auth.permission.roles1.policy=r1
                    """),
                    "application.properties"));

    @Inject
    InMemoryAuthTokenTestSender tokenSender;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("test", "test", "admin");
    }

    @BeforeEach
    public void cleanup() {
        tokenSender.clean();
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
                        RestAssuredMatchers.detailedCookie().value(containsString("/admin")).secured(false));

        var response = RestAssured
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
                .header("location", containsString("/authentication-token.html"))
                .extract();
        // no credentials as 2FA, OTAC is expected
        assertNull(response.cookie("quarkus-credential"));

        // still not authenticated
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .extract();
        assertNull(response.headers().get("quarkus-credential"));

        // use one-time authentication token
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/admin"))
                .cookie("quarkus-credential",
                        RestAssuredMatchers.detailedCookie().value(notNullValue()).secured(false));

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

        //now authenticate with a different user
        tokenSender.clean();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "test")
                .formParam("j_password", "test")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/authentication-token.html"));

        Awaitility.await().until(() -> tokenSender.getToken() != null);
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/landing"))
                .cookie("quarkus-credential",
                        RestAssuredMatchers.detailedCookie().value(notNullValue()).secured(false));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/admin"));

    }

    @Test
    public void testFormBasedAuthSuccessLandingPage() {
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
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
                .header("location", containsString("/authentication-token.html"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/landing"))
                .cookie("quarkus-credential", notNullValue());
    }

    @Test
    public void testFormAuthWrongPasswordFailure() {
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
    public void testFormAuthWrongTokenFailure() {
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
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
                .header("location", containsString("/authentication-token.html"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", "wrong_authentication_token")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/error"));
    }
}

package io.quarkus.vertx.http.security.form.token;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

/**
 * Test scenario when requests shouldn't be redirected, for example SPA will want to handle redirect itself.
 */
public class Form2FactorAuthNoRedirectTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    PathHandler.class, InMemoryAuthTokenTestSender.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.authentication-token.enabled=true
                    quarkus.http.auth.form.authentication-token.request-redirect-path=
                    quarkus.http.auth.form.authentication-token.cookie-name=custom-token-name
                    quarkus.http.auth.form.authentication-token.expires-in=3s
                    quarkus.http.auth.form.login-page=
                    quarkus.http.auth.form.error-page=
                    quarkus.http.auth.form.landing-page=
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
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.header("location"));
        assertNull(response.cookie("quarkus-credential"));
        assertNull(response.cookie("quarkus-credential-location"));

        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(204)
                .extract();
        // no redirect as 'quarkus.http.auth.form.authentication-token.request-redirect-path' is not set
        assertNull(response.headers().get("location"));
        // no credentials as 2FA, OTAC is expected
        assertNull(response.headers().get("quarkus-credential"));
        assertNotNull(response.cookie("custom-token-name"));

        // still not authenticated
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.cookie("quarkus-credential"));
        assertNull(response.headers().get("location"));

        // use one-time authentication token
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200)
                .extract();
        assertNotNull(response.cookie("quarkus-credential"));
        assertNull(response.headers().get("location"));

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
                .statusCode(204);

        Awaitility.await().until(() -> tokenSender.getToken() != null);
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200)
                .extract();
        assertNotNull(response.cookie("quarkus-credential"));
        assertNull(response.headers().get("location"));

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
                .statusCode(204)
                .extract();
        assertNull(response.headers().get("location"));
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        response = RestAssured
                .given()
                .filter(new CookieFilter())
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.header("location"));
        assertNull(response.cookie("quarkus-credential"));
    }

    @Test
    public void testCorrectTokenButNoCookie() {
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
                .statusCode(204)
                .extract();
        assertNull(response.headers().get("location"));
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200)
                .extract();
        assertNull(response.header("location"));
        assertNotNull(response.cookie("quarkus-credential"));
    }

    @Test
    public void testFormAuthWrongPasswordFailure() {
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "wrongpassword")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.header("location"));
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
                .statusCode(204)
                .extract();
        assertNull(response.headers().get("location"));
        assertNull(response.cookie("quarkus-credential"));
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", "wrong_authentication_token")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.header("location"));
    }

    @Test
    public void testFormAuthExpiredTokenFailure() throws InterruptedException {
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
                .statusCode(204)
                .extract();
        assertNull(response.headers().get("location"));
        assertNull(response.cookie("quarkus-credential"));
        Thread.sleep(5000);
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .extract();
        assertNull(response.header("location"));
    }
}

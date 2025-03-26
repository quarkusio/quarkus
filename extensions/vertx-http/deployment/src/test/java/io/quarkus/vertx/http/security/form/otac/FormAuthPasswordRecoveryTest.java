package io.quarkus.vertx.http.security.form.otac;

import static io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent.*;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent.FormEventType.ONE_TIME_AUTH_TOKEN_REQUESTED;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
import io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent;
import io.quarkus.vertx.http.security.PathHandler;
import io.quarkus.vertx.http.security.TestTrustedIdentityProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class FormAuthPasswordRecoveryTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    PathHandler.class, InMemoryAuthTokenTestSender.class, OneTimeTokenEventFailureObserver.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.authentication-token.enabled=true
                    quarkus.http.auth.form.authentication-token.request-path=/authentication-token
                    quarkus.http.auth.form.authentication-token.request-redirect-path=/authentication-token-form
                    quarkus.http.auth.form.authentication-token.form-parameter-name=one-time-token
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

    @Inject
    OneTimeTokenEventFailureObserver failureObserver;

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
    public void testLoginWithPassword() {
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
    public void testLoginWithOneTimeAuthToken_landingPage() {
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .post("/authentication-token")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/authentication-token-form"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("one-time-token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/landing"))
                .cookie("quarkus-credential", notNullValue());
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
    public void testLoginWithOneTimeAuthToken_redirectToAdmin() {
        CookieFilter cookies = new CookieFilter();
        testNotAuthenticated(cookies);
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .post("/authentication-token")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/authentication-token-form"))
                .extract();
        testNotAuthenticated(cookies);
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("one-time-token", new String(tokenSender.getToken()))
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/admin"))
                .cookie("quarkus-credential", notNullValue());
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
    public void testLoginFailureWhenWrongToken() {
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .post("/authentication-token")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/authentication-token-form"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));
        Awaitility.await().until(() -> tokenSender.getToken() != null);
        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("one-time-token", "wrong-wrong-wrong")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/error"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));
    }

    @Test
    public void testWrongUsernameInTokenRequest() {
        failureObserver.clear();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "unknown_username")
                .post("/authentication-token")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/authentication-token-form"));
        // we must not indicate that username is unknown with the response status, but the one-time authentication token
        // must not be generated
        Awaitility.await().until(() -> failureObserver.failureEvent != null);
        FormAuthenticationEvent event = failureObserver.failureEvent;
        String requestUsername = (String) event.getEventProperties().get(REQUEST_USERNAME);
        assertEquals("unknown_username", requestUsername);
    }

    @Test
    public void testMissingUsernameInTokenRequest() {
        failureObserver.clear();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .post("/authentication-token")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Singleton
    public static class OneTimeTokenEventFailureObserver {

        private volatile FormAuthenticationEvent failureEvent;

        void observeFailure(@Observes FormAuthenticationEvent event) {
            var props = event.getEventProperties();
            if (ONE_TIME_AUTH_TOKEN_REQUESTED.toString().equalsIgnoreCase((String) props.get(FORM_CONTEXT))
                    && props.get(AUTHENTICATION_FAILURE) != null) {
                this.failureEvent = event;
            }
        }

        void clear() {
            failureEvent = null;
        }

    }

    private static void testNotAuthenticated(CookieFilter cookies) {
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"));
    }
}

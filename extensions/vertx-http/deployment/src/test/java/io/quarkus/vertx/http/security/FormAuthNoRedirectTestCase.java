package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.*;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.ExpectLogMessage;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

@ExpectLogMessage(HttpSecurityRecorder.ENCRYPTION_KEY_WAS_NOT_SPECIFIED_FOR_PERSISTENT_FORM_AUTH)
public class FormAuthNoRedirectTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.form.redirect-after-login=false\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=a d m i n\n" +
            "quarkus.http.auth.permission.roles1.paths=/admin\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, TestTrustedIdentityProvider.class,
                            PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("a d m i n", "a d m i n", "a d m i n");
    }

    /**
     * First, protected /admin resource is accessed. No quarkus-credential cookie
     * is presented by the client, so server should redirect to /login page.
     *
     * Next, let's assume there was a login form on the /login page,
     * we do POST with valid credentials.
     * Server should provide a response with quarkus-credential cookie
     * and a redirect to the previously attempted /admin page.
     * Note the redirect takes place despite having quarkus.http.auth.form.redirect-after-login=false
     * because there is some previous location to redirect to.
     *
     * Last but not least, client accesses the protected /admin resource again,
     * this time providing server with stored quarkus-credential cookie.
     * Access is granted and landing page displayed.
     */
    @Test
    public void testFormBasedAuthSuccess() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
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
                .cookie("quarkus-redirect-location", containsString("/admin"));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "a d m i n")
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
                .body(equalTo("a d m i n:/admin"));
    }

    @Test
    public void testFormBasedAuthSuccessLandingPage() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "a d m i n")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200)
                .cookie("quarkus-credential", notNullValue());
    }

    @Test
    public void testFormAuthFailure() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "wrongpassword")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/error"))
                .header("quarkus-credential", nullValue());
    }
}

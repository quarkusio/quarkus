package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.matcher.RestAssuredMatchers;

public abstract class AbstractFormAuthTestCase {

    protected static final String APP_PROPS = "" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/admin\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("test", "test", "admin");
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
}

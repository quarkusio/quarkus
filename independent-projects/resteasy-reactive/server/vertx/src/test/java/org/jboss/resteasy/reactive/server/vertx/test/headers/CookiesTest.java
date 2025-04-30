package org.jboss.resteasy.reactive.server.vertx.test.headers;

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CookiesTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest TEST = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CookiesTestResource.class));

    @Test
    void testDefaults() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=hello;")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie()
                        .value("hello")
                        .version(1)
                        .secured(false)
                        .httpOnly(false)
                        .maxAge(-1)
                        .path(is(nullValue()))
                        .domain(is(nullValue()))
                        .comment(is(nullValue())));
    }

    @Test
    void testVersion0() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Version=\"0\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(0));
    }

    @Test
    void testVersion0WithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Version=\"0\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(0));
    }

    @Test
    void testVersion0Lowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";version=\"0\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(0));
    }

    @Test
    void testVersion1() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Version=\"1\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(1));
    }

    @Test
    void testVersion1WithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Version=\"1\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(1));
    }

    @Test
    void testVersion1Lowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";version=\"1\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").version(1));
    }

    @Test
    void testSameSite() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";SameSite=\"Lax\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").sameSite("Lax"));
    }

    @Test
    void testSameSiteWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";SameSite=\"None\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").sameSite("None"));
    }

    @Test
    void testSameSiteLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";samesite=\"Strict\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").sameSite("Strict"));
    }

    @Test
    void testHttpOnlyWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";HttpOnly")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").httpOnly(true));
    }

    @Test
    void testHttpOnly() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";HttpOnly;")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").httpOnly(true));
    }

    @Test
    void testHttpOnlyLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";httponly;")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").httpOnly(true));
    }

    @Test
    void testSecureWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Secure")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").secured(true));
    }

    @Test
    void testSecure() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Secure;")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").secured(true));
    }

    @Test
    void testSecureLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";secure;")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").secured(true));
    }

    @Test
    void testDomainWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Domain=\"quarkus.io\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").domain("quarkus.io"));
    }

    @Test
    void testDomain() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Secure;Domain=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").domain("quarkus.io"));
    }

    @Test
    void testDomainLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Secure;domain=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").domain("quarkus.io"));
    }

    @Test
    void testPathWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Path=\"quarkus.io\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").path("quarkus.io"));
    }

    @Test
    void testPath() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Path=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").path("quarkus.io"));
    }

    @Test
    void testPathLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";path=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").path("quarkus.io"));
    }

    @Test
    void testCommentWithoutColon() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Comment=\"quarkus.io\"")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").comment("quarkus.io"));
    }

    @Test
    void testComment() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";Comment=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").comment("quarkus.io"));
    }

    @Test
    void testCommentLowercase() {
        given()
                .when()
                .urlEncodingEnabled(true)
                .formParam("cookie", "greeting=\"hello\";comment=\"quarkus.io\";")
                .post("/cookies/set-cookie")
                .then()
                .cookie("greeting", detailedCookie().value("hello").comment("quarkus.io"));
    }

}

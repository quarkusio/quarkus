package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

/**
 * With disabled strict forwarded control, the Forwarded proto and the X-Forwarded-Proto header have order of precedence.
 * When validation for the forwarded proto is enabled, we do not allow invalid values, regardless of the precedence.
 */
class ForwardedProtoValidationBothHeadersTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ForwardedHandlerInitializer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-forwarded", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-x-forwarded", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.strict-forwarded-control", "false");

    @Test
    void testInvalidXForwardedProtoRejectedEvenWhenForwardedProtoIsValid() {
        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .header("X-Forwarded-Proto", "/bad")
                .get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void testInvalidForwardedProtoRejectedEvenWhenXForwardedProtoIsValid() {
        RestAssured.given()
                .header("Forwarded", "proto=/bad;for=backend:4444;host=somehost")
                .header("X-Forwarded-Proto", "https")
                .get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void testInvalidXForwardedProtoRejectedWhenForwardedHasNoProto() {
        RestAssured.given()
                .header("Forwarded", "for=backend:4444;host=somehost")
                .header("X-Forwarded-Proto", "/bad")
                .get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void testNoProtoInEitherHeader() {
        RestAssured.given()
                .header("Forwarded", "host=somehost")
                .header("X-Forwarded-For", "backend:4444")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("http|somehost|backend:4444"));
    }
}

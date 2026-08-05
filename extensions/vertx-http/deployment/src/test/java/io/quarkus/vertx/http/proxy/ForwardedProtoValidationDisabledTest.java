package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

class ForwardedProtoValidationDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ForwardedHandlerInitializer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-forwarded", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-x-forwarded", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.forwarded-proto-validation", "none");

    @Test
    void testInvalidProtosAcceptedWhenValidationDisabled() {
        RestAssured.given()
                .header("Forwarded", "proto=/bad/scheme;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("/bad/scheme|"));

        RestAssured.given()
                .header("X-Forwarded-Proto", "://")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("://|"));
    }
}

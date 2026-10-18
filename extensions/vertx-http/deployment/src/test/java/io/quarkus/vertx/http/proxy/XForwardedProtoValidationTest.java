package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

class XForwardedProtoValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ForwardedHandlerInitializer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true");

    @Test
    void testValidProtoValues() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "http")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("http|"));

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("https|"));

        RestAssured.given()
                .header("X-Forwarded-Proto", "ws.s+1")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("ws.s+1|"));
    }

    @Test
    void testInvalidProtoValues() {
        RestAssured.given()
                .header("X-Forwarded-Proto", "/just/a/path")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("X-Forwarded-Proto", "://")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("X-Forwarded-Proto", "1http")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("X-Forwarded-Proto", "htt:ps")
                .get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void testXForwardedSslStillWorks() {
        RestAssured.given()
                .header("X-Forwarded-Ssl", "on")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("https|"));
    }
}

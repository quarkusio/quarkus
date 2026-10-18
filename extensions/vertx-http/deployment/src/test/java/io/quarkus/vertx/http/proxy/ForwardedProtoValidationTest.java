package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

class ForwardedProtoValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ForwardedHandlerInitializer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-forwarded", "true");

    @Test
    void testValidProtoValues() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=http;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("http|"));

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("https|"));

        RestAssured.given()
                .header("Forwarded", "proto=h2c;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("h2c|"));

        RestAssured.given()
                .header("Forwarded", "proto=my.scheme+1;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("my.scheme+1|"));
    }

    @Test
    void testInvalidProtoValues() {
        RestAssured.given()
                .header("Forwarded", "proto=/just/a/path;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("Forwarded", "proto=://;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("Forwarded", "proto=1http;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("Forwarded", "proto=htt:ps;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(400);

        RestAssured.given()
                .header("Forwarded", "proto=ht tp;for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void testNoProtoHeaderStillWorks() {
        RestAssured.given()
                .header("Forwarded", "for=backend:4444;host=somehost")
                .get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("http|"));
    }
}

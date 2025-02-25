package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AllowForwardedAndXForwardedHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true\n" +
                            "quarkus.http.proxy.allow-x-forwarded=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n" +
                            "quarkus.http.proxy.forwarded-host-header=X-Forwarded-Server"),
                            "application.properties"));

    @Test
    public void testAllHeaderValuesMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend2:5555")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|somehost2|backend2:5555|/path|https://somehost2/path"));
    }

    @Test
    public void tesProtoHeaderValuesMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "https")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|somehost2|backend2:5555|/path|https://somehost2/path"));
    }

    @Test
    public void testForHeaderValuesMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-For", "backend2:5555")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|somehost2|backend2:5555|/path|https://somehost2/path"));
    }

    @Test
    public void testHostHeaderValuesMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|somehost2|backend2:5555|/path|https://somehost2/path"));
    }

    @Test
    public void testProtoDoesNotMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "http")
                .header("X-Forwarded-For", "backend2:5555")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .statusCode(400);
    }

    @Test
    public void testForHostDoesNotMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:5555;host=somehost2")
                .header("X-Forwarded-Proto", "http")
                .header("X-Forwarded-For", "backend2:5555")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .statusCode(400);
    }

    @Test
    public void testForHostPortDoesNotMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:4444;host=somehost2")
                .header("X-Forwarded-Proto", "http")
                .header("X-Forwarded-For", "backend2:5555")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .statusCode(400);
    }

    @Test
    public void testHostDoesNotMatch() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:4444;host=somehost")
                .header("X-Forwarded-Proto", "http")
                .header("X-Forwarded-For", "backend2:5555")
                .header("X-Forwarded-Server", "somehost2")
                .get("/path")
                .then()
                .statusCode(400);
    }
}
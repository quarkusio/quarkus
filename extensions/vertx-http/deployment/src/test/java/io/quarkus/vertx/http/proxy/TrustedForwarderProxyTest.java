package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

public class TrustedForwarderProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n" +
                            "quarkus.http.proxy.trusted-proxies=localhost"),
                            "application.properties"));

    @Test
    public void testHeadersAreUsed() {
        RestAssured.given()
                .header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .get("/path")
                .then()
                .body(Matchers.equalTo("http|somehost2|backend2:5555|/path|http://somehost2/path"));
    }

    @Test
    public void testHeadersAreUsedWithTrustedProxyHeader() {
        RestAssured.given()
                .header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .get("/path-trusted-proxy")
                .then()
                .body(Matchers
                        .equalTo("http|somehost2|backend2:5555|/path-trusted-proxy|http://somehost2/path-trusted-proxy|null"));
    }

    @Test
    public void testWithoutTrustedProxyHeader() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|null"));
    }

    @Test
    public void testThatTrustedProxyHeaderCannotBeForged() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "true")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|null"));

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "hello")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|null"));

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "false")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|null"));
    }

    /**
     * As described on <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded</a>,
     * the syntax should be case-insensitive.
     * <p>
     * Kong, for example, uses `Proto` instead of `proto` and `For` instead of `for`.
     */
    @Test
    public void testHeadersAreUsedWhenUsingCasedCharacters() {
        RestAssured.given()
                .header("Forwarded", "Proto=http;For=backend2:5555;Host=somehost2")
                .get("/path")
                .then()
                .body(Matchers.equalTo("http|somehost2|backend2:5555|/path|http://somehost2/path"));
    }
}

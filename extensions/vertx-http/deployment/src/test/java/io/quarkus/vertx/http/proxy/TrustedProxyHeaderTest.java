package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

/**
 * Test the trusted-proxy header
 */
public class TrustedProxyHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.proxy.proxy-address-forwarding=true
                            quarkus.http.proxy.allow-forwarded=true
                            quarkus.http.proxy.enable-forwarded-host=true
                            quarkus.http.proxy.enable-forwarded-prefix=true
                            quarkus.http.proxy.allow-forwarded=true
                            quarkus.http.proxy.enable-trusted-proxy-header=true
                            quarkus.http.proxy.trusted-proxies=localhost
                            """),
                            "application.properties"));

    @Test
    public void testHeadersAreUsed() {
        RestAssured.given()
                .header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .get("/path-trusted-proxy")
                .then()
                .body(Matchers
                        .equalTo("http|somehost2|backend2:5555|/path-trusted-proxy|http://somehost2/path-trusted-proxy|true"));
    }

    @Test
    public void testTrustedProxyHeader() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|true"));
    }

    @Test
    public void testThatTrustedProxyHeaderCannotBeForged() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "true")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|true"));

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "hello")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|true"));

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .header("X-Forwarded-Trusted-Proxy", "false")
                .get("/trusted-proxy")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|true"));
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
                .get("/path-trusted-proxy")
                .then()
                .body(Matchers
                        .equalTo("http|somehost2|backend2:5555|/path-trusted-proxy|http://somehost2/path-trusted-proxy|true"));
    }
}

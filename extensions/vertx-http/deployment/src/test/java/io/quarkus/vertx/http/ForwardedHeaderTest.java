package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ForwardedHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true"),
                            "application.properties"));

    @Test
    public void test() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444;host=somehost;proto=https")
                .get("/forward")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444"));
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

    @Test
    public void testForwardedForWithSequenceOfProxies() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=backend:4444,for=backend2:5555;host=somehost;proto=https")
                .get("/forward")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444"));
    }

    @Test
    public void testForwardedWithSequenceOfProxiesIncludingIpv6Address() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=\"[2001:db8:cafe::17]:47011\",for=backend:4444;host=somehost;proto=https")
                .get("/forward")
                .then()
                .body(Matchers.equalTo("https|somehost|[2001:db8:cafe::17]:47011"));
    }

    @Test
    public void testForwardedForWithIpv6Address2() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "by=proxy;for=\"[2001:db8:cafe::17]:47011\",for=backend:4444;host=somehost;proto=https")
                .get("/forward")
                .then()
                .body(Matchers.equalTo("https|somehost|[2001:db8:cafe::17]:47011"));
    }
}

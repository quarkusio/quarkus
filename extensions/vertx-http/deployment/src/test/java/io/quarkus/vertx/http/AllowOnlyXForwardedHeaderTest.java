package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AllowOnlyXForwardedHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(ForwardedHandlerInitializer.class)
            .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n"
                    + "quarkus.http.proxy.allow-x-forwarded=true\n" + "quarkus.http.proxy.enable-forwarded-host=true\n"
                    + "quarkus.http.proxy.enable-forwarded-prefix=true\n"
                    + "quarkus.http.proxy.forwarded-host-header=X-Forwarded-Server"), "application.properties"));

    @Test
    public void testWithXForwardedSslOn() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given().header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Ssl", "on").header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Server", "somehost").get("/path").then()
                .body(Matchers.equalTo("https|somehost|backend:4444|/path|https://somehost/path"));
    }

    @Test
    public void testWithXForwardedProto() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given().header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Server", "somehost").get("/path").then()
                .body(Matchers.equalTo("https|somehost|backend:4444|/path|https://somehost/path"));
    }

    @Test
    public void testWithXForwardedProtoAndXForwardedSsl() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given().header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "https").header("X-Forwarded-Ssl", "on")
                .header("X-Forwarded-For", "backend:4444").header("X-Forwarded-Server", "somehost").get("/path").then()
                .body(Matchers.equalTo("https|somehost|backend:4444|/path|https://somehost/path"));
    }
}

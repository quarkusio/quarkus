package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ForwardedPrefixHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n"),
                            "application.properties"));

    @Test
    public void test() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "/prefix")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444|/prefix/path|https://localhost/prefix/path"));
    }

    @Test
    public void testWithASlashAtEnding() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "/prefix/")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444|/prefix/path|https://localhost/prefix/path"));
    }

    @Test
    public void testWhenPrefixIsEmpty() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444|/path|https://localhost/path"));
    }

    @Test
    public void testWhenPrefixIsASlash() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "/")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444|/path|https://localhost/path"));
    }

    @Test
    public void testWhenPrefixIsADoubleSlash() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "//")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444|/path|https://localhost/path"));
    }

}

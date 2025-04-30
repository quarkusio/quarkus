package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AllowXForwardedHeadersOverrideForwardedHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true\n" +
                            "quarkus.http.proxy.allow-x-forwarded=true\n" +
                            "quarkus.http.proxy.forwarded-precedence=x-forwarded\n" +
                            "quarkus.http.proxy.strict-forwarded-control=false\n"),
                            "application.properties"));

    @Test
    public void testXForwardedProtoOverridesForwardedProto() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Proto", "http")
                .get("/path")
                .then()
                .body(Matchers.equalTo("http|somehost2|backend2:5555|/path|http://somehost2/path"));
    }

}
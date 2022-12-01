package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConfigureForwardedHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n" +
                            "quarkus.http.proxy.forwarded-host-header=X-Forwarded-Server\n" +
                            "quarkus.http.proxy.forwarded-prefix-header=X-Envoy-Path\n"),
                            "application.properties"));

    @Test
    public void test() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Server", "somehost")
                .header("X-Envoy-Path", "/prefix")
                .get("/path")
                .then()
                .body(Matchers.equalTo("https|somehost|backend:4444|/prefix/path|https://somehost/prefix/path"));
    }

}

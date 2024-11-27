package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AllowXForwardedHostTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.proxy.proxy-address-forwarding=true\n"
                                    + "quarkus.http.proxy.enable-forwarded-host=true\n"),
                            "application.properties"));

    @Test
    public void testXForwardedProtoOverridesForwardedProto() {
        assertThat(RestAssured.get("/path").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Host", "foo")
                .get("/path")
                .then()
                .body(
                        Matchers.startsWith("http|foo:8081|"),
                        Matchers.endsWith("|/path|http://foo:8081/path"));
    }

}
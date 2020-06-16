package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ProxyAddressForwardingDefaultSettingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy-address-forwarding=true\n"),
                            "application.properties"));

    @Test
    public void testWithHostHeader() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Host", "somehost")
                .header("X-Forwarded-Prefix", "prefix")
                .get("/forward")
                .then()
                .body(Matchers.equalTo("https|localhost|backend:4444"));
    }

    @Test
    public void testWithPrefixHeader() {
        RestAssured.given()
                .header("X-Forwarded-Prefix", "prefix")
                .get("/uri")
                .then()
                .body(Matchers.equalTo("/uri"));
    }
}

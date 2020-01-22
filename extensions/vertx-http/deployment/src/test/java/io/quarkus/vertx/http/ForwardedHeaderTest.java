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

public class ForwardedHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy-address-forwarding=true\n" +
                            "quarkus.http.allow-forwarded=true"),
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

}

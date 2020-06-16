package io.quarkus.vertx.http;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnvoyHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy-address-forwarding=true\n"
                            + "quarkus.http.forwarded-prefix-header=X-Envoy-Original-Path\n"),
                            "application.properties"));

    @Test
    public void test() {
        RestAssured.given()
                .header("X-Envoy-Original-Path", "prefix")
                .get("/uri")
                .then()
                .body(Matchers.equalTo("/prefix/uri"));
    }

    @Test
    public void testWithEmptyPrefix() {
        RestAssured.given()
                .header("X-Envoy-Original-Path", "")
                .get("/uri")
                .then()
                .body(Matchers.equalTo("/uri"));
    }

}

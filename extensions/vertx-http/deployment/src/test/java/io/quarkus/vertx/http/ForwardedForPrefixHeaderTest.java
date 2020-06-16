package io.quarkus.vertx.http;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ForwardedForPrefixHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy-address-forwarding=true\n"
                            + "quarkus.http.forwarded-prefix-header=X-Forwarded-Prefix\n"),
                            "application.properties"));

    @Test
    public void testWithPrefix() {
        RestAssured.given()
                .header("X-Forwarded-Prefix", "prefix")
                .get("/uri")
                .then()
                .body(Matchers.equalTo("/prefix/uri"));
    }

    @Test
    public void testWithEmptyPrefix() {
        RestAssured.given()
                .header("X-Forwarded-Prefix", "")
                .get("/uri")
                .then()
                .body(Matchers.equalTo("/uri"));
    }

}

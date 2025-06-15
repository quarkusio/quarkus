package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

public class TrustedForwarderProxyFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(ForwardedHandlerInitializer.class)
            .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n"
                    + "quarkus.http.proxy.allow-forwarded=true\n" + "quarkus.http.proxy.enable-forwarded-host=true\n"
                    + "quarkus.http.proxy.enable-forwarded-prefix=true\n"
                    + "quarkus.http.proxy.trusted-proxies=alnoenlqoe334219384nvfeoslcxnxeoanelnsoe9.gov"),
                    "application.properties"));

    @Test
    public void testHeadersAreIgnored() {
        RestAssured.given()
                .header("Forwarded",
                        "by=proxy;for=\"[2001:db8:cafe::17]:47011\",for=backend:4444;host=somehost;proto=https")
                .get("/forward").then().body(Matchers.startsWith("http|localhost:8081|127.0.0.1:"));
    }
}

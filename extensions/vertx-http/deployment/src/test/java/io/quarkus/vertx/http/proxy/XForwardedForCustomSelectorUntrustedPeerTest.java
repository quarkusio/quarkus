package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

class XForwardedForCustomSelectorUntrustedPeerTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ForwardedHandlerInitializer.class, CustomXForwardedForSelector.class))
            .withConfiguration("""
                    quarkus.http.proxy.proxy-address-forwarding=true
                    quarkus.http.proxy.allow-x-forwarded=true
                    quarkus.http.proxy.trusted-proxies=10.20.30.40
                    """);

    @Test
    void doesNotInvokeSelectorForUntrustedPeer() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Reject", "true").get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("127.0.0.1"))
                .body(Matchers.not(Matchers.containsString("1.2.3.4")));
    }
}

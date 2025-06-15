package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractTrustedXForwarderProxiesTest {

    static final String SUCCESS = "https|somehost|backend:4444|/path|https://somehost/path";

    protected static QuarkusUnitTest createTrustedProxyUnitTest(String... trustedProxies) {
        final String trustedProxiesAsStr;
        if (trustedProxies.length == 0) {
            trustedProxiesAsStr = "";
        } else {
            trustedProxiesAsStr = "quarkus.http.proxy.trusted-proxies=" + String.join(",", trustedProxies) + "\n";
        }
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar.addClasses(ForwardedHandlerInitializer.class).addAsResource(
                        new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n"
                                + "quarkus.http.proxy.allow-x-forwarded=true\n"
                                + "quarkus.http.proxy.enable-forwarded-host=true\n"
                                + "quarkus.http.proxy.enable-forwarded-prefix=true\n" + trustedProxiesAsStr
                                + "quarkus.http.proxy.forwarded-host-header=X-Forwarded-Server"),
                        "application.properties"));
    }

    protected static ValidatableResponse request() {
        return RestAssured.given().header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .header("X-Forwarded-Ssl", "on").header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Server", "somehost").get("/path").then();
    }

    static void assertRequestSuccess() {
        assertRequestSuccess(request());
    }

    static void assertRequestSuccess(ValidatableResponse request) {
        request.body(Matchers.equalTo(SUCCESS));
    }

    static void assertRequestFailure() {
        assertRequestFailure(request());
    }

    static void assertRequestFailure(ValidatableResponse request) {
        request
                // we don't check port of 127.0.0.1 as that's subject to change
                .body(Matchers.startsWith("http|localhost:8081|127.0.0.1:"))
                .body(Matchers.endsWith("|/path|http://localhost:8081/path"));
        // without 'quarkus.http.proxy.trusted-proxies=1.2.3.4' config property
        // response would be: 'https|somehost|backend:4444|/path|https://somehost/path'
    }
}

package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.response.ValidatableResponse;

class TrustedProxyDnRequiredFailureTest extends AbstractTrustedProxyDnTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTest("REQUIRED", "CN=my-trusted-proxy");

    @Test
    void testForwardedWithWrongDnIgnored() {
        assertUntrusted(forwardedRequest());
    }

    @Test
    void testXForwardedWithWrongDnIgnored() {
        assertUntrusted(xForwardedRequest());
    }

    @Test
    void testTrustedProxyHeaderCannotBeForged() {
        assertUntrusted(forwardedRequestWithForgedTrustedHeader());
    }

    private ValidatableResponse forwardedRequestWithForgedTrustedHeader() {
        return mTlsRequest()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .header("X-Forwarded-Trusted-Proxy", "true")
                .get(tlsUrl)
                .then()
                .statusCode(200);
    }
}

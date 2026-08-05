package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class TrustedProxyDnRequestFailureTest extends AbstractTrustedProxyDnTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTest("REQUEST", "CN=my-trusted-proxy");

    @Test
    void testForwardedWithWrongDnIgnored() {
        assertUntrusted(forwardedRequest());
    }

    @Test
    void testXForwardedWithWrongDnIgnored() {
        assertUntrusted(xForwardedRequest());
    }
}

package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class TrustedProxyDnRequiredTest extends AbstractTrustedProxyDnTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTest("REQUIRED", "CN=localhost,O=not-matching", "CN=localhost");

    @Test
    void testForwardedWithMatchingDn() {
        assertTrusted(forwardedRequest());
    }

    @Test
    void testXForwardedWithMatchingDn() {
        assertTrusted(xForwardedRequest());
    }
}

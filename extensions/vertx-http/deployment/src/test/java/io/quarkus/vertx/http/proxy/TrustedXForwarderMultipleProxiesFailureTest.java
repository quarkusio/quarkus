package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TrustedXForwarderMultipleProxiesFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTrustedProxyUnitTest("1.2.3.4", "quarkus.io", "154.5.128.0/17",
            "::ffff:154.6.99.64/123");

    @Test
    public void testHeadersAreIgnored() {
        assertRequestFailure();
    }

}

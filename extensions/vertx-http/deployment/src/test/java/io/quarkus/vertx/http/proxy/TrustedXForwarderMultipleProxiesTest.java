package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TrustedXForwarderMultipleProxiesTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = createTrustedProxyUnitTest("1.2.3.4", "quarkus.io", "vertx.io",
            "154.6.0.0/15", "::ffff:154.6.99.64/123", "localhost");

    @Test
    public void testHeadersAreUsed() {
        // request should succeed as localhost check matches
        assertRequestSuccess();
    }

}

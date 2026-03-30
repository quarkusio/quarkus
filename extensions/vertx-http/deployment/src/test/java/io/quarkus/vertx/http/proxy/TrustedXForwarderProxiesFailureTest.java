package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TrustedXForwarderProxiesFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTrustedProxyUnitTest("1.2.3.4");

    @Test
    public void testHeadersAreIgnored() {
        assertRequestFailure();
    }

}

package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TrustedXForwarderProxiesHostnameFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTrustedProxyUnitTest("quarkus.io");

    @Test
    public void testHeadersAreIgnored() {
        assertRequestFailure();
    }

}

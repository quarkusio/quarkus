package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TrustedXForwarderProxiesIPTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTrustedProxyUnitTest("127.0.0.1");

    @Test
    public void testHeadersAreUsed() {
        assertRequestSuccess();
    }

}

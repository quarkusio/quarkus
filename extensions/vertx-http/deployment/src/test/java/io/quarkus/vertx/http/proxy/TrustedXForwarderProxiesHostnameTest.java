package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TrustedXForwarderProxiesHostnameTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = createTrustedProxyUnitTest("localhost");

    @Test
    public void testHeadersAreUsed() {
        assertRequestSuccess();
    }

}

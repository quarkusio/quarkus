package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TrustedXForwarderProxiesHostnameFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = createTrustedProxyUnitTest("quarkus.io");

    @Test
    public void testHeadersAreIgnored() {
        assertRequestFailure();
    }

}

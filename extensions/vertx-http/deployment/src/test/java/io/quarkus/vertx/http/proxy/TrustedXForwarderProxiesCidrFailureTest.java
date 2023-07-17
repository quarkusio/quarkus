package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TrustedXForwarderProxiesCidrFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = createTrustedProxyUnitTest("::1/128");

    @Test
    public void testHeadersAreIgnored() {
        // headers are ignored as request is sent from IPv4
        assertRequestFailure();
    }
}

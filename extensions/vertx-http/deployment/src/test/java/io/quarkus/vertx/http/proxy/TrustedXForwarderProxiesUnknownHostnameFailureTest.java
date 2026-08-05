package io.quarkus.vertx.http.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TrustedXForwarderProxiesUnknownHostnameFailureTest extends AbstractTrustedXForwarderProxiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = createTrustedProxyUnitTest(
            // let's hope this domain is not registered
            "alnoenlkepdolndqoe334219384nvfeoslcxnxeoanelnsoe9.gov");

    @Test
    public void testHeadersAreIgnored() {
        assertRequestFailure();
    }

}

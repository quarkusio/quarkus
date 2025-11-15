package io.quarkus.rest.client.reactive.proxy.config.legacy;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractGlobalProxyPasswordTest;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalProxyPasswordTest extends AbstractGlobalProxyPasswordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = config("global-proxy-password-test-application.properties");

}

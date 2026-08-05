package io.quarkus.rest.client.reactive.proxy.config.legacy;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractSystemPropertyProxyTest;
import io.quarkus.test.QuarkusExtensionTest;

public class SystemPropertyProxyTest extends AbstractSystemPropertyProxyTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = config("system-props-proxy-test-application.properties");
}

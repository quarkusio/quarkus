package io.quarkus.rest.client.reactive.proxy.config.legacy;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractSystemPropertyProxyWithAuthTest;
import io.quarkus.test.QuarkusExtensionTest;

public class SystemPropertyProxyWithAuthTest extends AbstractSystemPropertyProxyWithAuthTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = config("system-props-proxy-test-application.properties");

}

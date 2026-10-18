package io.quarkus.rest.client.reactive.proxy.config.registry;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractSystemPropertyProxyWithAuthTest;
import io.quarkus.test.QuarkusExtensionTest;

public class SystemPropertyProxyRegistryWithAuthTest extends AbstractSystemPropertyProxyWithAuthTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = config("system-props-proxy-registry-test-application.properties");

}

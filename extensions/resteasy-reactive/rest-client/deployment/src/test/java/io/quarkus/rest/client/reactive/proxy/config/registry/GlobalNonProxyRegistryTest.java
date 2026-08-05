package io.quarkus.rest.client.reactive.proxy.config.registry;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractGlobalNonProxyTest;
import io.quarkus.test.QuarkusExtensionTest;

public class GlobalNonProxyRegistryTest extends AbstractGlobalNonProxyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = config("global-non-proxy-registry-test-application.properties");

}

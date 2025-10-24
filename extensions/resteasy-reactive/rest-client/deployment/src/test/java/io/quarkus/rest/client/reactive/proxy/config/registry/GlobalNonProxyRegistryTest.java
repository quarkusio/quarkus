package io.quarkus.rest.client.reactive.proxy.config.registry;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractGlobalNonProxyTest;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalNonProxyRegistryTest extends AbstractGlobalNonProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = config("global-non-proxy-registry-test-application.properties");

}

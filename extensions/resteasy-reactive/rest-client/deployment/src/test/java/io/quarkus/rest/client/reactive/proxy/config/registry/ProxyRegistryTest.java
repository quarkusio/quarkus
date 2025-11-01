package io.quarkus.rest.client.reactive.proxy.config.registry;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractProxyTest;
import io.quarkus.test.QuarkusUnitTest;

/**
 * client1 and client2 are configured to use 8181 as a proxy, global configuration says to use 8182
 */
public class ProxyRegistryTest extends AbstractProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = config("proxy-registry-test-application.properties");
}

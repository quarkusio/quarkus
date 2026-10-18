package io.quarkus.rest.client.reactive.proxy.config.registry;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractGlobalProxyPasswordTest;
import io.quarkus.test.QuarkusExtensionTest;

public class GlobalProxyRegistryPasswordTest extends AbstractGlobalProxyPasswordTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = config("global-proxy-registry-password-test-application.properties");

}

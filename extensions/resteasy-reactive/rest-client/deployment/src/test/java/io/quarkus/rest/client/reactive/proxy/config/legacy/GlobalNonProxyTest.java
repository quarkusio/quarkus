package io.quarkus.rest.client.reactive.proxy.config.legacy;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractGlobalNonProxyTest;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalNonProxyTest extends AbstractGlobalNonProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = config("global-non-proxy-test-application.properties");

}

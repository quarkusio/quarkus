package io.quarkus.rest.client.reactive.proxy.config.legacy;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.proxy.AbstractSystemPropertyProxyWithAuthTest;
import io.quarkus.test.QuarkusUnitTest;

public class SystemPropertyProxyWithAuthTest extends AbstractSystemPropertyProxyWithAuthTest {
    @RegisterExtension
    static final QuarkusUnitTest config = config("system-props-proxy-test-application.properties");

}

package io.quarkus.proxy.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultProxyMissingTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ProxyConfigurationRegistry registry;

    @Test
    public void test() {
        Optional<ProxyConfiguration> proxy = registry.get(Optional.empty());
        assertTrue(proxy.isEmpty());
    }
}

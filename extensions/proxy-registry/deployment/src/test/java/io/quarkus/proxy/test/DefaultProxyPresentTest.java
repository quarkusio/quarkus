package io.quarkus.proxy.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.ProxyType;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultProxyPresentTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("default-proxy-present.properties");

    @Inject
    ProxyConfigurationRegistry registry;

    @Test
    public void test() {
        Optional<ProxyConfiguration> proxy = registry.get(Optional.empty());
        assertTrue(proxy.isPresent());
        ProxyConfiguration cfg = proxy.get();
        assertEquals("localhost", cfg.host());
        assertEquals(3128, cfg.port());
        assertEquals(Optional.of("user"), cfg.username());
        assertEquals(Optional.of("pwd"), cfg.password());
        assertEquals(Optional.of(List.of("localhost", "example.com")), cfg.nonProxyHosts());
        assertEquals(Optional.of(Duration.ofSeconds(1)), cfg.proxyConnectTimeout());
        assertEquals(ProxyType.HTTP, cfg.type());
    }
}

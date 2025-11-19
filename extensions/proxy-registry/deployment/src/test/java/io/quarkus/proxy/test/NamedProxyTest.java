package io.quarkus.proxy.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

public class NamedProxyTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("named-proxy.properties");

    @Inject
    ProxyConfigurationRegistry registry;

    @Test
    public void testPresent() {
        Optional<ProxyConfiguration> proxy = registry.get(Optional.of("my-proxy"));
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

    @Test
    public void testMissing() {
        assertThrows(IllegalStateException.class, () -> registry.get(Optional.of("missing")));
    }

    @Test
    public void testNone() {
        Optional<ProxyConfiguration> proxy = registry.get(Optional.of("none"));
        assertTrue(proxy.isPresent());
        ProxyConfiguration cfg = proxy.get();
        assertEquals("none", cfg.host());
        assertEquals(0, cfg.port());
        assertEquals(Optional.empty(), cfg.username());
        assertEquals(Optional.empty(), cfg.password());
        assertEquals(Optional.empty(), cfg.nonProxyHosts());
        assertEquals(Optional.empty(), cfg.proxyConnectTimeout());
        assertEquals(ProxyType.HTTP, cfg.type());
    }
}

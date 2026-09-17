package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Proxy;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.ProxyType;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;

public class OidcProxyConfigurationTest {

    private static final ProxyConfigurationRegistry REGISTRY = name -> {
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return switch (name.get()) {
            case ProxyConfigurationRegistry.NONE -> Optional.of(proxy("none", 0, Optional.empty()));
            case "plain" -> Optional.of(proxy("proxy.local", 3128, Optional.empty()));
            case "with-exclusions" -> Optional
                    .of(proxy("proxy.local", 3128, Optional.of(List.of("localhost", "*.internal"))));
            default -> throw new IllegalStateException(
                    "Proxy configuration with name " + name.get() + " was requested but no such configuration exists");
        };
    };

    @Test
    public void testNoneNameMeansNoProxy() {
        assertTrue(OidcCommonUtils.toProxyOptions(named(ProxyConfigurationRegistry.NONE), REGISTRY).isEmpty());
    }

    @Test
    public void testUnknownNameIsReportedAsConfigurationError() {
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.toProxyOptions(named("unknown"), REGISTRY));
        assertTrue(e.getMessage().contains("unknown"), e.getMessage());
    }

    @Test
    public void testNamedProxyIsApplied() {
        HttpClientOptions options = new HttpClientOptions();
        OidcCommonUtils.configureProxy(named("plain"), options, REGISTRY);
        ProxyOptions proxyOptions = options.getProxyOptions();
        assertEquals("proxy.local", proxyOptions.getHost());
        assertEquals(3128, proxyOptions.getPort());
        assertTrue(options.getNonProxyHosts() == null || options.getNonProxyHosts().isEmpty());
    }

    @Test
    public void testNonProxyHostsAreApplied() {
        HttpClientOptions options = new HttpClientOptions();
        OidcCommonUtils.configureProxy(named("with-exclusions"), options, REGISTRY);
        assertEquals("proxy.local", options.getProxyOptions().getHost());
        assertEquals(List.of("localhost", "*.internal"), options.getNonProxyHosts());
    }

    @Test
    public void testNoneNameLeavesClientOptionsWithoutProxy() {
        HttpClientOptions options = new HttpClientOptions();
        OidcCommonUtils.configureProxy(named(ProxyConfigurationRegistry.NONE), options, REGISTRY);
        assertNull(options.getProxyOptions());
    }

    private static Proxy named(String name) {
        return () -> Optional.of(name);
    }

    private static ProxyConfiguration proxy(String host, int port, Optional<List<String>> nonProxyHosts) {
        return new ProxyConfiguration() {
            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return port;
            }

            @Override
            public Optional<String> username() {
                return Optional.empty();
            }

            @Override
            public Optional<String> password() {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> nonProxyHosts() {
                return nonProxyHosts;
            }

            @Override
            public Optional<Duration> proxyConnectTimeout() {
                return Optional.empty();
            }

            @Override
            public ProxyType type() {
                return ProxyType.HTTP;
            }
        };
    }
}

package io.quarkus.keycloak.pep.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.ProxyType;
import io.quarkus.runtime.configuration.ConfigurationException;

public class KeycloakPolicyEnforcerProxyTest {

    private static final String AUTH_SERVER_URL = "https://keycloak.local/realms/quarkus";

    private static final ProxyConfigurationRegistry REGISTRY = name -> {
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return switch (name.get()) {
            case ProxyConfigurationRegistry.NONE -> Optional.of(proxy("none", 0, Optional.empty()));
            case "plain" -> Optional.of(proxy("proxy.local", 3128, Optional.empty()));
            case "with-exclusions" -> Optional.of(proxy("proxy.local", 3128, Optional.of(List.of("keycloak.local"))));
            default -> throw new IllegalStateException(
                    "Proxy configuration with name " + name.get() + " was requested but no such configuration exists");
        };
    };

    @Test
    public void testNamedProxyIsUsed() {
        assertEquals(Optional.of("https://proxy.local:3128"),
                KeycloakPolicyEnforcerUtil.getProxyUrl(Optional.of("plain"), AUTH_SERVER_URL, REGISTRY));
    }

    @Test
    public void testNoNameAndNoneUseNoProxy() {
        assertEquals(Optional.empty(), KeycloakPolicyEnforcerUtil.getProxyUrl(Optional.empty(), AUTH_SERVER_URL, REGISTRY));
        assertEquals(Optional.empty(), KeycloakPolicyEnforcerUtil.getProxyUrl(Optional.of(ProxyConfigurationRegistry.NONE),
                AUTH_SERVER_URL, REGISTRY));
    }

    @Test
    public void testNonProxyHostsAreRejected() {
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> KeycloakPolicyEnforcerUtil.getProxyUrl(Optional.of("with-exclusions"), AUTH_SERVER_URL, REGISTRY));
        assertTrue(e.getMessage().contains("quarkus.proxy.with-exclusions.non-proxy-hosts"), e.getMessage());
    }

    @Test
    public void testUnknownNameIsReportedAsConfigurationError() {
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> KeycloakPolicyEnforcerUtil.getProxyUrl(Optional.of("unknown"), AUTH_SERVER_URL, REGISTRY));
        assertTrue(e.getMessage().contains("unknown"), e.getMessage());
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

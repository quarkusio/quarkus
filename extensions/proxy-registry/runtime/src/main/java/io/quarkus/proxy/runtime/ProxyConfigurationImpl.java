package io.quarkus.proxy.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyType;

record ProxyConfigurationImpl(
        String host,
        int port,
        Optional<String> username,
        Optional<String> password,
        Optional<List<String>> nonProxyHosts,
        Optional<Duration> proxyConnectTimeout,
        ProxyType type) implements ProxyConfiguration {
}

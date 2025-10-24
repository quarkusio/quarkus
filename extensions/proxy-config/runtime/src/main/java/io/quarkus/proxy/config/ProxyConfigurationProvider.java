package io.quarkus.proxy.config;

import java.util.Optional;

public interface ProxyConfigurationProvider {
    Optional<String> proxyConfigurationName();
}

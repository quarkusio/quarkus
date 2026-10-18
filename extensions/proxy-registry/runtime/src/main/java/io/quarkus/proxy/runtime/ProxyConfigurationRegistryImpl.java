package io.quarkus.proxy.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.ProxyType;

public class ProxyConfigurationRegistryImpl implements ProxyConfigurationRegistry {
    private final Map<String, ProxyConfiguration> configs;
    private final Optional<ProxyConfiguration> defaultConfig;

    ProxyConfigurationRegistryImpl(Map<String, ProxyConfiguration> configs, Optional<ProxyConfiguration> defaultConfig) {
        this.configs = configs;
        this.defaultConfig = defaultConfig;
    }

    @Override
    public Optional<ProxyConfiguration> get(Optional<String> name) {
        if (name.isEmpty()) {
            return defaultConfig;
        }

        String key = name.get();
        if (ProxyConfigurationRegistry.NONE.equals(key)) {
            return NONE;
        }

        ProxyConfiguration config = configs.get(key);
        if (config == null) {
            throw new IllegalStateException("Proxy configuration with name " + key + " was requested but "
                    + "quarkus.proxy.\"" + key + "\".host is not defined");
        }
        return Optional.of(config);
    }

    private static final Optional<ProxyConfiguration> NONE = Optional.of(new ProxyConfigurationImpl("none", 0,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), ProxyType.HTTP));
}

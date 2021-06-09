package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultCacheEntry.tryReturnLastKnownValue;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.vault.VaultKVSecretEngine;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class VaultConfigSource implements ConfigSource {

    private static final Logger log = Logger.getLogger(VaultConfigSource.class);

    private AtomicReference<VaultCacheEntry<Map<String, String>>> cache = new AtomicReference<>(null);
    private VaultBootstrapConfig vaultBootstrapConfig;

    public VaultConfigSource(VaultBootstrapConfig vaultBootstrapConfig) {
        this.vaultBootstrapConfig = vaultBootstrapConfig;
    }

    @Override
    public String getName() {
        return VaultBootstrapConfig.NAME;
    }

    @Override
    public int getOrdinal() {
        return vaultBootstrapConfig.configOrdinal;
    }

    /**
     * always return an empty map to protect from accidental properties logging
     *
     * @return empty map
     */
    @Override
    public Map<String, String> getProperties() {
        return emptyMap();
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public String getValue(String propertyName) {
        return vaultBootstrapConfig.url.isPresent() ? getSecretConfig().get(propertyName) : null;
    }

    private Map<String, String> getSecretConfig() {

        VaultCacheEntry<Map<String, String>> cacheEntry = cache.get();
        if (cacheEntry != null && cacheEntry.youngerThan(vaultBootstrapConfig.secretConfigCachePeriod)) {
            return cacheEntry.getValue();
        }

        if (!Infrastructure.canCallerThreadBeBlocked()) {
            // running in a non blocking thread, best effort to return cached values if any
            return cacheEntry != null ? cacheEntry.getValue() : Collections.emptyMap();
        }

        Map<String, String> properties = new HashMap<>();

        try {
            // default kv paths
            vaultBootstrapConfig.secretConfigKvPath.ifPresent(strings -> fetchSecrets(strings, null, properties));

            // prefixed kv paths
            vaultBootstrapConfig.secretConfigKvPathPrefix.forEach((key, value) -> fetchSecrets(value.paths, key, properties));

            log.debug("loaded " + properties.size() + " properties from vault");
        } catch (RuntimeException e) {
            return tryReturnLastKnownValue(e, cacheEntry);
        }

        cache.set(new VaultCacheEntry(properties));
        return properties;

    }

    private void fetchSecrets(List<String> paths, String prefix, Map<String, String> properties) {
        paths.forEach(path -> properties.putAll(fetchSecrets(path, prefix)));
    }

    private Map<String, String> fetchSecrets(String path, String prefix) {
        return prefixMap(getVaultKVSecretEngine().readSecret(path), prefix);
    }

    private VaultKVSecretEngine getVaultKVSecretEngine() {
        return Arc.container().instance(VaultKVSecretEngine.class).get();
    }

    private Map<String, String> prefixMap(Map<String, String> map, String prefix) {
        return prefix == null
                ? map
                : map.entrySet().stream().collect(toMap(entry -> prefix + "." + entry.getKey(), Map.Entry::getValue));
    }
}

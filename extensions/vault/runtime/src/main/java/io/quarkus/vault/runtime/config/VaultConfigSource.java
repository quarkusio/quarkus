package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultCacheEntry.tryReturnLastKnownValue;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.VaultManager;

public class VaultConfigSource implements ConfigSource {

    private static final Logger log = Logger.getLogger(VaultConfigSource.class);

    private AtomicReference<VaultCacheEntry<Map<String, String>>> cache = new AtomicReference<>(null);
    private VaultRuntimeConfig serverConfig;

    private int ordinal;

    public VaultConfigSource(int ordinal, VaultRuntimeConfig vaultRuntimeConfig) {
        this.ordinal = ordinal;
        this.serverConfig = vaultRuntimeConfig;
    }

    @Override
    public String getName() {
        return VaultRuntimeConfig.NAME;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
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
    public String getValue(String propertyName) {
        return serverConfig.url.isPresent() ? getSecretConfig().get(propertyName) : null;
    }

    private Map<String, String> getSecretConfig() {

        VaultCacheEntry<Map<String, String>> cacheEntry = cache.get();
        if (cacheEntry != null && cacheEntry.youngerThan(serverConfig.secretConfigCachePeriod)) {
            return cacheEntry.getValue();
        }

        Map<String, String> properties = new HashMap<>();

        try {
            // default kv paths
            serverConfig.secretConfigKvPath.ifPresent(strings -> fetchSecrets(strings, null, properties));

            // prefixed kv paths
            serverConfig.secretConfigKvPathPrefix.forEach((key, value) -> fetchSecrets(value.paths, key, properties));

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
        return prefixMap(VaultManager.getInstance().getVaultKvManager().readSecret(path), prefix);
    }

    private Map<String, String> prefixMap(Map<String, String> map, String prefix) {
        return prefix == null
                ? map
                : map.entrySet().stream().collect(toMap(entry -> prefix + "." + entry.getKey(), Map.Entry::getValue));
    }
}

package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultCacheEntry.tryReturnLastKnownValue;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.VaultManager;

class VaultConfigSource implements ConfigSource {

    private static final Logger log = Logger.getLogger(VaultConfigSource.class);

    private AtomicReference<VaultCacheEntry<Map<String, String>>> cache = new AtomicReference<>(null);
    private AtomicReference<VaultRuntimeConfig> serverConfig = new AtomicReference<>(null);
    private AtomicReference<VaultBuildTimeConfig> buildServerConfig = new AtomicReference<>(null);

    private AtomicBoolean init = new AtomicBoolean(false);
    private int ordinal;

    public VaultConfigSource(int ordinal, VaultBuildTimeConfig vaultBuildTimeConfig, VaultRuntimeConfig vaultRuntimeConfig) {
        this.ordinal = ordinal;
        this.buildServerConfig.set(vaultBuildTimeConfig);
        this.serverConfig.set(vaultRuntimeConfig);

        System.out.println("DEBUG VAULT ===> stringListMap = " + vaultRuntimeConfig.stringListMap);
        System.out.println("DEBUG VAULT ===> stringMap = " + vaultRuntimeConfig.stringMap);
        System.out.println("DEBUG VAULT ===> stringList = " + vaultRuntimeConfig.stringList);

        System.out.println("DEBUG VAULT ===> secretConfigKvPath = " + vaultRuntimeConfig.secretConfigKvPath);
        System.out.println("DEBUG VAULT ===> secretConfigKvPrefixPath = " + vaultRuntimeConfig.secretConfigKvPrefixPath);
        System.out.println("DEBUG VAULT ===> credentialsProvider = " + vaultRuntimeConfig.credentialsProvider);

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

        VaultRuntimeConfig serverConfig = this.serverConfig.get();

        if (!serverConfig.url.isPresent()) {
            return null;
        }

        return getSecretConfig().get(propertyName);
    }

    private Map<String, String> getSecretConfig() {

        VaultRuntimeConfig serverConfig = this.serverConfig.get();

        VaultCacheEntry<Map<String, String>> cacheEntry = cache.get();
        if (cacheEntry != null && cacheEntry.youngerThan(serverConfig.secretConfigCachePeriod)) {
            return cacheEntry.getValue();
        }

        Map<String, String> properties = new HashMap<>();

        try {
            // default kv paths
            if (serverConfig.secretConfigKvPath.isPresent()) {
                fetchSecrets(serverConfig.secretConfigKvPath.get(), null, properties);
            }

            // prefixed kv paths
            serverConfig.secretConfigKvPrefixPath.entrySet()
                    .forEach(entry -> fetchSecrets(entry.getValue(), entry.getKey(), properties));

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
        VaultManager instance = getVaultManager();
        return instance == null ? emptyMap() : prefixMap(instance.getVaultKvManager().readSecret(path), prefix);
    }

    private Map<String, String> prefixMap(Map<String, String> map, String prefix) {
        return prefix == null
                ? map
                : map.entrySet().stream().collect(toMap(entry -> prefix + "." + entry.getKey(), Map.Entry::getValue));
    }

    // ---

    private VaultManager getVaultManager() {

        VaultBuildTimeConfig buildTimeConfig = this.buildServerConfig.get();
        VaultRuntimeConfig serverConfig = this.serverConfig.get();

        // init at most once
        if (init.compareAndSet(false, true)) {
            VaultManager.init(buildTimeConfig, serverConfig);
        }

        return VaultManager.getInstance();
    }

}

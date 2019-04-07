package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.MEDIUM;
import static io.quarkus.vault.runtime.config.VaultCacheEntry.tryReturnLastKnownValue;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_CONNECT_TIMEOUT;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_KUBERNETES_JWT_TOKEN_PATH;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_READ_TIMEOUT;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_RENEW_GRACE_PERIOD;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_SECRET_CONFIG_CACHE_PERIOD;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_TLS_SKIP_VERIFY;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_TLS_USE_KUBERNETES_CACERT;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KV_SECRET_ENGINE_VERSION_V1;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.LogConfidentialityLevel;
import io.quarkus.vault.runtime.VaultManager;

public class VaultConfigSource implements ConfigSource {

    private static final Logger log = Logger.getLogger(VaultConfigSource.class);

    private static final String PROPERTY_PREFIX = "quarkus.vault.";
    public static final Pattern CREDENTIAL_PATTERN = Pattern.compile("^quarkus\\.vault\\.credentials-provider\\.([^.]+)\\.");

    private AtomicReference<VaultCacheEntry<Map<String, String>>> cache = new AtomicReference<>(null);
    private AtomicReference<VaultRuntimeConfig> serverConfig = new AtomicReference<>(null);
    private AtomicBoolean init = new AtomicBoolean(false);
    private int ordinal;
    private DurationConverter durationConverter = new DurationConverter();

    public VaultConfigSource(int ordinal) {
        this.ordinal = ordinal;
    }

    @Override
    public String getName() {
        return "vault";
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
        return Collections.emptyMap();
    }

    @Override
    public String getValue(String propertyName) {

        VaultRuntimeConfig serverConfig = getConfig();

        if (!serverConfig.url.isPresent()) {
            return null;
        }

        return getSecretConfig().get(propertyName);
    }

    private Map<String, String> getSecretConfig() {

        VaultRuntimeConfig serverConfig = getConfig();

        VaultCacheEntry<Map<String, String>> cacheEntry = cache.get();
        if (cacheEntry != null && cacheEntry.youngerThan(serverConfig.secretConfigCachePeriod)) {
            return cacheEntry.getValue();
        }

        Map<String, String> properties = new HashMap<>();

        if (serverConfig.secretConfigKvPath.isPresent()) {
            try {
                properties.putAll(fetchSecrets(serverConfig));
                log.debug("loaded " + properties.size() + " properties from vault");
            } catch (RuntimeException e) {
                return tryReturnLastKnownValue(e, cacheEntry);
            }
        }

        cache.set(new VaultCacheEntry(properties));
        return properties;

    }

    private Map<String, String> fetchSecrets(VaultRuntimeConfig serverConfig) {
        VaultManager instance = getVaultManager();
        return instance == null
                ? Collections.emptyMap()
                : instance.getVaultKvManager().readSecret(serverConfig.secretConfigKvPath.get());
    }

    // ---

    private VaultManager getVaultManager() {

        VaultRuntimeConfig serverConfig = getConfig();

        // init at most once
        if (init.compareAndSet(false, true)) {
            VaultManager.init(serverConfig);
        }

        return VaultManager.getInstance();
    }

    private VaultRuntimeConfig getConfig() {
        VaultRuntimeConfig serverConfig = this.serverConfig.get();
        if (serverConfig != null) {
            return serverConfig;
        } else {
            serverConfig = loadConfig();
            log.debug("loaded vault server config " + serverConfig);
            this.serverConfig.set(serverConfig);
            return this.serverConfig.get();
        }
    }

    // need to recode config loading since we are at the config source level
    private VaultRuntimeConfig loadConfig() {

        VaultRuntimeConfig serverConfig = new VaultRuntimeConfig();
        serverConfig.tls = new VaultTlsConfig();
        serverConfig.authentication = new VaultAuthenticationConfig();
        serverConfig.authentication.userpass = new VaultUserpassAuthenticationConfig();
        serverConfig.authentication.appRole = new VaultAppRoleAuthenticationConfig();
        serverConfig.authentication.kubernetes = new VaultKubernetesAuthenticationConfig();
        serverConfig.url = newURL(getOptionalVaultProperty("url"));
        serverConfig.authentication.clientToken = getOptionalVaultProperty("authentication.client-token");
        serverConfig.authentication.kubernetes.role = getOptionalVaultProperty("authentication.kubernetes.role");
        serverConfig.authentication.kubernetes.jwtTokenPath = getVaultProperty("authentication.kubernetes.jwt-token-path",
                DEFAULT_KUBERNETES_JWT_TOKEN_PATH);
        serverConfig.authentication.userpass.username = getOptionalVaultProperty("authentication.userpass.username");
        serverConfig.authentication.userpass.password = getOptionalVaultProperty("authentication.userpass.password");
        serverConfig.authentication.appRole.roleId = getOptionalVaultProperty("authentication.app-role.role-id");
        serverConfig.authentication.appRole.secretId = getOptionalVaultProperty("authentication.app-role.secret-id");
        serverConfig.renewGracePeriod = getVaultDuration("renew-grace-period", DEFAULT_RENEW_GRACE_PERIOD);
        serverConfig.secretConfigCachePeriod = getVaultDuration("secret-config-cache-period",
                DEFAULT_SECRET_CONFIG_CACHE_PERIOD);
        serverConfig.logConfidentialityLevel = LogConfidentialityLevel
                .valueOf(getVaultProperty("log-confidentiality-level", MEDIUM.name()).toUpperCase());
        serverConfig.kvSecretEngineVersion = parseInt(
                getVaultProperty("kv-secret-engine-version", KV_SECRET_ENGINE_VERSION_V1));
        serverConfig.kvSecretEngineMountPath = getVaultProperty("kv-secret-engine-mount-path",
                DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH);
        serverConfig.secretConfigKvPath = getOptionalVaultProperty("secret-config-kv-path");
        serverConfig.tls.skipVerify = parseBoolean(getVaultProperty("tls.skip-verify", DEFAULT_TLS_SKIP_VERIFY));
        serverConfig.tls.useKubernetesCaCert = parseBoolean(
                getVaultProperty("tls.use-kubernetes-ca-cert", DEFAULT_TLS_USE_KUBERNETES_CACERT));
        serverConfig.tls.caCert = getOptionalVaultProperty("tls.ca-cert");
        serverConfig.connectTimeout = getVaultDuration("connect-timeout", DEFAULT_CONNECT_TIMEOUT);
        serverConfig.readTimeout = getVaultDuration("read-timeout", DEFAULT_READ_TIMEOUT);

        serverConfig.credentialsProvider = getCredentialsProviders();

        return serverConfig;
    }

    private Optional<URL> newURL(Optional<String> url) {
        try {
            return Optional.ofNullable(url.isPresent() ? new URL(url.get()) : null);
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }

    private Optional<String> getOptionalVaultProperty(String key) {
        return Optional.ofNullable(getVaultProperty(key, null));
    }

    private Duration getVaultDuration(String key, String defaultValue) {
        return durationConverter.convert(getVaultProperty(key, defaultValue));
    }

    private String getVaultProperty(String key, String defaultValue) {
        String propertyName = PROPERTY_PREFIX + key;

        return getConfigSourceStream()
                .map(configSource -> configSource.getValue(propertyName))
                .filter(value -> value != null && value.length() != 0)
                .map(String::trim)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * returns a map of all credentials providers
     *
     * @return credential poviders by name
     */
    private Map<String, CredentialsProviderConfig> getCredentialsProviders() {

        return getConfigSourceStream()
                .flatMap(configSource -> configSource.getPropertyNames().stream())
                .map(this::getCredentialsProviderName)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::createNameCredentialsProviderPair)
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private Stream<ConfigSource> getConfigSourceStream() {
        Config config = ConfigProviderResolver.instance().getConfig();
        return StreamSupport.stream(config.getConfigSources().spliterator(), false).filter(this::retain);
    }

    private boolean retain(ConfigSource configSource) {
        String other;
        try {
            other = configSource.getName();
        } catch (NullPointerException e) {
            // FIXME at org.jboss.resteasy.microprofile.config.BaseServletConfigSource.getName(BaseServletConfigSource.java:51)
            other = null;
        }
        return !getName().equals(other);
    }

    private SimpleEntry<String, CredentialsProviderConfig> createNameCredentialsProviderPair(String name) {
        return new SimpleEntry<>(name, getCredentialsProviderConfig(name));
    }

    private String getCredentialsProviderName(String propertyName) {
        Matcher matcher = CREDENTIAL_PATTERN.matcher(propertyName);
        return matcher.find() ? matcher.group(1) : null;
    }

    private CredentialsProviderConfig getCredentialsProviderConfig(String name) {
        String prefix = "credentials-provider." + name;
        CredentialsProviderConfig config = new CredentialsProviderConfig();
        config.databaseCredentialsRole = getOptionalVaultProperty(prefix + ".database-credentials-role");
        config.kvPath = getOptionalVaultProperty(prefix + ".kv-path");
        config.kvKey = getVaultProperty(prefix + ".kv-key", PASSWORD_PROPERTY_NAME);
        return config;
    }

}

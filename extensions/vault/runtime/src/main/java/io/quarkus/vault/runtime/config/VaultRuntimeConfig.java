package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.MEDIUM;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vault.runtime.LogConfidentialityLevel;

@ConfigRoot(name = VaultRuntimeConfig.NAME, phase = ConfigPhase.BOOTSTRAP)
public class VaultRuntimeConfig {

    public static final String NAME = "vault";
    public static final String DEFAULT_KUBERNETES_JWT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    public static final String DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH = "secret";
    public static final String KV_SECRET_ENGINE_VERSION_V2 = "2";
    public static final String DEFAULT_RENEW_GRACE_PERIOD = "1H";
    public static final String DEFAULT_SECRET_CONFIG_CACHE_PERIOD = "10M";
    public static final String KUBERNETES_CACERT = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    public static final String DEFAULT_CONNECT_TIMEOUT = "5S";
    public static final String DEFAULT_READ_TIMEOUT = "1S";
    public static final String DEFAULT_TLS_USE_KUBERNETES_CACERT = "true";
    public static final String DEFAULT_KUBERNETES_AUTH_MOUNT_PATH = "auth/kubernetes";

    /**
     * Vault server url.
     *
     * Example: https://localhost:8200
     *
     * See also the documentation for the `kv-secret-engine-mount-path` property for some insights on how
     * the full Vault url gets built.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<URL> url;

    /**
     * Authentication
     */
    @ConfigItem
    @ConfigDocSection
    public VaultAuthenticationConfig authentication;

    /**
     * Renew grace period duration.
     *
     * This value if used to extend a lease before it expires its ttl, or recreate a new lease before the current
     * lease reaches its max_ttl.
     * By default Vault leaseDuration is equal to 7 days (ie: 168h or 604800s).
     * If a connection pool maxLifetime is set, it is reasonable to set the renewGracePeriod to be greater
     * than the maxLifetime, so that we are sure we get a chance to renew leases before we reach the ttl.
     * In any case you need to make sure there will be attempts to fetch secrets within the renewGracePeriod,
     * because that is when the renewals will happen. This is particularly important for db dynamic secrets
     * because if the lease reaches its ttl or max_ttl, the password of the db user will become invalid and
     * it will be not longer possible to log in.
     * This value should also be smaller than the ttl, otherwise that would mean that we would try to recreate
     * leases all the time.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = DEFAULT_RENEW_GRACE_PERIOD)
    public Duration renewGracePeriod;

    /**
     * Vault config source cache period.
     *
     * Properties fetched from vault as MP config will be kept in a cache, and will not be fetched from vault
     * again until the expiration of that period.
     * This property is ignored if `secret-config-kv-path` is not set.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = DEFAULT_SECRET_CONFIG_CACHE_PERIOD)
    public Duration secretConfigCachePeriod;

    // @formatter:off
    /**
     * List of comma separated vault paths in kv store,
     * where all properties will be available as MP config properties **as-is**, with no prefix.
     *
     * For instance, if vault contains property `foo`, it will be made available to the
     * quarkus application as `@ConfigProperty(name = "foo") String foo;`
     *
     * If 2 paths contain the same property, the last path will win.
     *
     * For instance if
     *
     * * `secret/base-config` contains `foo=bar` and
     * * `secret/myapp/config` contains `foo=myappbar`, then
     *
     * `@ConfigProperty(name = "foo") String foo` will have value `myappbar`
     * with application properties `quarkus.vault.secret-config-kv-path=base-config,myapp/config`
     *
     * See also the documentation for the `kv-secret-engine-mount-path` property for some insights on how
     * the full Vault url gets built.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    public Optional<List<String>> secretConfigKvPath;

    /**
     * KV store paths configuration.
     */
    @ConfigItem(name = "secret-config-kv-path")
    @ConfigDocMapKey("prefix")
    public Map<String, KvPathConfig> secretConfigKvPathPrefix;

    /**
     * Used to hide confidential infos, for logging in particular.
     * Possible values are:
     *
     * * low: display all secrets.
     * * medium: display only usernames and lease ids (ie: passwords and tokens are masked).
     * * high: hide lease ids and dynamic credentials username.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "medium")
    public LogConfidentialityLevel logConfidentialityLevel;

    /**
     * Kv secret engine version.
     *
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = KV_SECRET_ENGINE_VERSION_V2)
    public int kvSecretEngineVersion;

    /**
     * KV secret engine path.
     *
     * This value is used when building the url path in the KV secret engine programmatic access
     * (i.e. `VaultKVSecretEngine`) and the vault config source (i.e. fetching configuration properties from Vault).
     *
     * For a v2 KV secret engine (default - see `kv-secret-engine-version property`)
     * the full url is built from the expression `<url>/v1/</kv-secret-engine-mount-path>/data/...`.
     *
     * With property `quarkus.vault.url=https://localhost:8200`, the following call
     * `vaultKVSecretEngine.readSecret("foo/bar")` would lead eventually to a `GET` on Vault with the following
     * url: `https://localhost:8200/v1/secret/data/foo/bar`.
     *
     * With a KV secret engine v1, the url changes to: `<url>/v1/</kv-secret-engine-mount-path>/...`.
     *
     * The same logic is applied to the Vault config source. With `quarkus.vault.secret-config-kv-path=config/myapp`
     * The secret properties would be fetched from Vault using a `GET` on
     * `https://localhost:8200/v1/secret/data/config/myapp` for a KV secret engine v2 (or
     * `https://localhost:8200/v1/secret/config/myapp` for a KV secret engine v1).
     *
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH)
    public String kvSecretEngineMountPath;

    /**
     * TLS
     */
    @ConfigItem
    @ConfigDocSection
    public VaultTlsConfig tls;

    /**
     * Timeout to establish a connection with Vault.
     */
    @ConfigItem(defaultValue = DEFAULT_CONNECT_TIMEOUT)
    public Duration connectTimeout;

    /**
     * Request timeout on Vault.
     */
    @ConfigItem(defaultValue = DEFAULT_READ_TIMEOUT)
    public Duration readTimeout;

    /**
     * List of named credentials providers, such as: `quarkus.vault.credentials-provider.foo.kv-path=mypath`
     *
     * This defines a credentials provider `foo` returning key `password` from vault path `mypath`.
     * Once defined, this provider can be used in credentials consumers, such as the Agroal connection pool.
     *
     * Example: `quarkus.datasource.credentials-provider=foo`
     *
     * @asciidoclet
     */
    @ConfigItem
    public Map<String, CredentialsProviderConfig> credentialsProvider;

    /**
     * Transit Engine
     */
    @ConfigItem
    @ConfigDocSection
    public VaultTransitConfig transit;

    public VaultAuthenticationType getAuthenticationType() {
        if (authentication.kubernetes.role.isPresent()) {
            return KUBERNETES;
        } else if (authentication.isUserpass()) {
            return USERPASS;
        } else if (authentication.isAppRole()) {
            return APPROLE;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "VaultRuntimeConfig{" +
                "url=" + url +
                ", kubernetesAuthenticationMountPath=" + authentication.kubernetes.authMountPath +
                ", kubernetesAuthenticationRole="
                + logConfidentialityLevel.maskWithTolerance(authentication.kubernetes.role.orElse(""), MEDIUM) +
                ", kubernetesJwtTokenPath='" + authentication.kubernetes.jwtTokenPath + '\'' +
                ", userpassUsername='"
                + logConfidentialityLevel.maskWithTolerance(authentication.userpass.username.orElse(""), MEDIUM)
                + '\'' +
                ", userpassPassword='"
                + logConfidentialityLevel.maskWithTolerance(authentication.userpass.password.orElse(""), LOW) + '\''
                +
                ", appRoleRoleId='"
                + logConfidentialityLevel.maskWithTolerance(authentication.appRole.roleId.orElse(""), MEDIUM) + '\'' +
                ", appRoleSecretId='"
                + logConfidentialityLevel.maskWithTolerance(authentication.appRole.secretId.orElse(""), LOW) + '\'' +
                ", appRoleSecretIdWrappingToken='"
                + logConfidentialityLevel.maskWithTolerance(authentication.appRole.secretIdWrappingToken.orElse(""), LOW) + '\''
                +
                ", clientToken=" + logConfidentialityLevel.maskWithTolerance(authentication.clientToken.orElse(""), LOW) +
                ", clientTokenWrappingToken="
                + logConfidentialityLevel.maskWithTolerance(authentication.clientTokenWrappingToken.orElse(""), LOW) +
                ", renewGracePeriod=" + renewGracePeriod +
                ", cachePeriod=" + secretConfigCachePeriod +
                ", logConfidentialityLevel=" + logConfidentialityLevel +
                ", kvSecretEngineVersion=" + kvSecretEngineVersion +
                ", kvSecretEngineMountPath='" + kvSecretEngineMountPath + '\'' +
                ", tlsSkipVerify=" + tls.skipVerify +
                ", tlsCaCert=" + tls.caCert +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }

    @ConfigGroup
    public static class KvPathConfig {
        // @formatter:off
        /**
         * List of comma separated vault paths in kv store,
         * where all properties will be available as **prefixed** MP config properties.
         *
         * For instance if the application properties contains
         * `quarkus.vault.secret-config-kv-path.myprefix=config`, and
         * vault path `secret/config` contains `foo=bar`, then `myprefix.foo`
         * will be available in the MP config.
         *
         * If the same property is available in 2 different paths for the same prefix, the last one
         * will win.
         *
         * See also the documentation for the `quarkus.vault.kv-secret-engine-mount-path` property for some insights on how
         * the full Vault url gets built.
         *
         * @asciidoclet
         */
        // @formatter:on
        @ConfigItem(name = ConfigItem.PARENT)
        List<String> paths;

        public KvPathConfig(List<String> paths) {
            this.paths = paths;
        }

        public KvPathConfig() {
            paths = Collections.emptyList();
        }

        @Override
        public String toString() {
            return "SecretConfigKvPathConfig{" +
                    "paths=" + paths +
                    '}';
        }
    }
}

package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.MEDIUM;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vault.runtime.LogConfidentialityLevel;

@ConfigRoot(name = "vault", phase = ConfigPhase.RUN_TIME)
public class VaultRuntimeConfig {

    public static final String DEFAULT_KUBERNETES_JWT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    public static final String DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH = "secret";
    public static final String KV_SECRET_ENGINE_VERSION_V1 = "1";
    public static final String KV_SECRET_ENGINE_VERSION_V2 = "2";
    public static final String DEFAULT_RENEW_GRACE_PERIOD = "1H";
    public static final String DEFAULT_SECRET_CONFIG_CACHE_PERIOD = "10M";
    public static final String KUBERNETES_CACERT = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    public static final String DEFAULT_CONNECT_TIMEOUT = "5S";
    public static final String DEFAULT_READ_TIMEOUT = "1S";
    public static final String DEFAULT_TLS_SKIP_VERIFY = "false";
    public static final String DEFAULT_TLS_USE_KUBERNETES_CACERT = "true";

    /**
     * Vault server url.
     * <p>
     * Example: https://localhost:8200
     */
    @ConfigItem
    public Optional<URL> url;

    /**
     * Authentication type when logging in to get a Vault client token.
     * <p>
     * Possible values are:
     * <ul>
     * <li>kubernetes: Kubernetes authentication as defined in https://www.vaultproject.io/api/auth/kubernetes/index.html</li>
     * <li>userpass: user/password authentication as defined in https://www.vaultproject.io/api/auth/userpass/index.html</li>
     * <li>app-role: role/secret authentication as defined in https://www.vaultproject.io/api/auth/approle/index.html</li>
     * </ul>
     * The actual type is determined automatically based on sub-properties quarkus.vault.authentication.*
     */
    @ConfigItem
    public VaultAuthenticationConfig authentication;

    /**
     * Renew grace period duration.
     * <p>
     * This value if used to extend a lease before it expires its ttl, or recreate a new lease before the current
     * lease reaches its max_ttl.
     * By default Vault leaseDuration is equal to 7 days (ie: 168h or 604800s).
     * If a connection pool maxLifetime is set, it is reasonable to set the renewGracePeriod to be greater
     * than the maxLifetime, so that we are sure we get a chance to renew leases before we reach the ttl.
     * In any case you need to make sure there will be attempts to fetch secrets within the renewGracePeriod,
     * because that is when the renewals will happen. This particularly important for db dynamic secrets
     * because if the lease reaches its ttl or max_ttl, the password of the db user will become invalid and
     * it will be not longer possible to log in.
     * This value should also be smaller than the ttl, otherwise that would mean that we would try to recreate
     * leases all the time.
     */
    @ConfigItem(defaultValue = DEFAULT_RENEW_GRACE_PERIOD)
    public Duration renewGracePeriod;

    /**
     * Vault config source cache period.
     * <p>
     * Properties fetched from vault as MP config will be kept in a cache, and will not be fetched from vault
     * again until the expiration of that period.
     * This property is ignored if secret-config-kv-path is not set.
     */
    @ConfigItem(defaultValue = DEFAULT_SECRET_CONFIG_CACHE_PERIOD)
    public Duration secretConfigCachePeriod;

    /**
     * Vault path in kv store, where all properties will be available as MP config.
     */
    @ConfigItem
    public Optional<String> secretConfigKvPath;

    /**
     * Used to hide confidential infos, for logging in particular.
     * Possible values are:
     * <li>
     * <ul>
     * low: display all secrets.
     * </ul>
     * <ul>
     * medium: display only usernames and lease ids (ie: passwords and tokens are masked).
     * </ul>
     * <ul>
     * high: hide lease ids and dynamic credentials username.
     * </ul>
     * </li>
     */
    @ConfigItem(defaultValue = "medium")
    public LogConfidentialityLevel logConfidentialityLevel;

    /**
     * Kv secret engine version.
     * <p>
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     */
    @ConfigItem(defaultValue = KV_SECRET_ENGINE_VERSION_V1)
    public int kvSecretEngineVersion;

    /**
     * Kv secret engine path.
     * <p>
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     */
    @ConfigItem(defaultValue = DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH)
    public String kvSecretEngineMountPath;

    /**
     * Tls config
     */
    @ConfigItem
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
     * List of named credentials providers, such as: quarkus.vault.credentials-provider.foo.kv-path=mypath
     * <p>
     * This defines a credentials provider 'foo' returning key 'password' from vault path 'mypath'.
     * Once defined, this provider can be used in credentials consumers, such as the Agroal connection pool.
     * <p>
     * Example: quarkus.datasource.credentials-provider=foo
     */
    @ConfigItem
    public Map<String, CredentialsProviderConfig> credentialsProvider;

    public VaultAuthenticationType getAuthenticationType() {
        if (authentication.kubernetes.role.isPresent()) {
            return KUBERNETES;
        } else if (authentication.userpass.username.isPresent() && authentication.userpass.password.isPresent()) {
            return USERPASS;
        } else if (authentication.appRole.roleId.isPresent() && authentication.appRole.secretId.isPresent()) {
            return APPROLE;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "VaultRuntimeConfig{" +
                "url=" + url +
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
                ", clientToken=" + logConfidentialityLevel.maskWithTolerance(authentication.clientToken.orElse(""), LOW) +
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
}

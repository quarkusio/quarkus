package io.quarkus.oidc.common.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcCommonConfig {
    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * Do not set this property if you use 'quarkus-oidc' and the public key verification ({@link #publicKey})
     * or certificate chain verification only ({@link #certificateChain}) is required.
     * The OIDC discovery endpoint is called by default by appending a `.well-known/openid-configuration` path to this URL.
     * For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.
     */
    @ConfigItem
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Discovery of the OIDC endpoints.
     * If not enabled, you must configure the OIDC endpoint URLs individually.
     */
    @ConfigItem(defaultValueDocumentation = "true")
    public Optional<Boolean> discoveryEnabled = Optional.empty();

    /**
     * The relative path or absolute URL of the OIDC dynamic client registration endpoint.
     * Set if {@link #discoveryEnabled} is `false` or a discovered token endpoint path must be customized.
     */
    @ConfigItem
    public Optional<String> registrationPath = Optional.empty();

    /**
     * The duration to attempt the initial connection to an OIDC server.
     * For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart.
     * This property is only effective when the initial OIDC connection is created.
     * For dropped connections, use the `connection-retry-count` property instead.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost.
     * Different from `connection-delay`, which applies only to initial connection attempts.
     * For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this
     * setting.
     */
    @ConfigItem(defaultValue = "3")
    public int connectionRetryCount = 3;

    /**
     * The number of seconds after which the current OIDC connection request times out.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Whether DNS lookup should be performed on the worker thread.
     * Use this option when you can see logged warnings about blocked Vert.x event loop by HTTP requests to OIDC server.
     */
    @ConfigItem(defaultValue = "false")
    public boolean useBlockingDnsLookup;

    /**
     * The maximum size of the connection pool used by the WebClient.
     */
    @ConfigItem
    public OptionalInt maxPoolSize = OptionalInt.empty();

    /**
     * Options to configure the proxy the OIDC adapter uses to talk with the OIDC server.
     */
    @ConfigItem
    public Proxy proxy = new Proxy();

    /**
     * TLS configurations
     */
    @ConfigItem
    public Tls tls = new Tls();

    @ConfigGroup
    public static class Tls {

        /**
         * The name of the TLS configuration to use.
         * <p>
         * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
         * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
         * <p>
         * The default TLS configuration is <strong>not</strong> used by default.
         */
        @ConfigItem
        Optional<String> tlsConfigurationName = Optional.empty();

        public enum Verification {
            /**
             * Certificates are validated and hostname verification is enabled. This is the default value.
             */
            REQUIRED,

            /**
             * Certificates are validated but hostname verification is disabled.
             */
            CERTIFICATE_VALIDATION,

            /**
             * All certificates are trusted and hostname verification is disabled.
             */
            NONE
        }

        /**
         * Certificate validation and hostname verification, which can be one of the following {@link Verification}
         * values.
         * Default is `required`.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<Verification> verification = Optional.empty();

        /**
         * An optional keystore that holds the certificate information instead of specifying separate files.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<Path> keyStoreFile = Optional.empty();

        /**
         * The type of the keystore file. If not given, the type is automatically detected based on the file name.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> keyStoreFileType = Optional.empty();

        /**
         * The provider of the keystore file. If not given, the provider is automatically detected based on the
         * keystore file type.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> keyStoreProvider;

        /**
         * The password of the keystore file. If not given, the default value, `password`, is used.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> keyStorePassword;

        /**
         * The alias of a specific key in the keystore.
         * When SNI is disabled, if the keystore contains multiple
         * keys and no alias is specified, the behavior is undefined.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> keyStoreKeyAlias = Optional.empty();

        /**
         * The password of the key, if it is different from the {@link #keyStorePassword}.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> keyStoreKeyPassword = Optional.empty();

        /**
         * The truststore that holds the certificate information of the certificates to trust.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<Path> trustStoreFile = Optional.empty();

        /**
         * The password of the truststore file.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> trustStorePassword = Optional.empty();

        /**
         * The alias of the truststore certificate.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> trustStoreCertAlias = Optional.empty();

        /**
         * The type of the truststore file.
         * If not given, the type is automatically detected
         * based on the file name.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> trustStoreFileType = Optional.empty();

        /**
         * The provider of the truststore file.
         * If not given, the provider is automatically detected
         * based on the truststore file type.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        @ConfigItem
        public Optional<String> trustStoreProvider;

        public Optional<Verification> getVerification() {
            return verification;
        }

        public void setVerification(Verification verification) {
            this.verification = Optional.of(verification);
        }

        public Optional<Path> getTrustStoreFile() {
            return trustStoreFile;
        }

        public void setTrustStoreFile(Path trustStoreFile) {
            this.trustStoreFile = Optional.of(trustStoreFile);
        }

        public Optional<String> getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = Optional.of(trustStorePassword);
        }

        public Optional<String> getTrustStoreCertAlias() {
            return trustStoreCertAlias;
        }

        public void setTrustStoreCertAlias(String trustStoreCertAlias) {
            this.trustStoreCertAlias = Optional.of(trustStoreCertAlias);
        }

        public Optional<String> getKeyStoreProvider() {
            return keyStoreProvider;
        }

        public void setKeyStoreProvider(String keyStoreProvider) {
            this.keyStoreProvider = Optional.of(keyStoreProvider);
        }

        public Optional<String> getTrustStoreProvider() {
            return trustStoreProvider;
        }

        public void setTrustStoreProvider(String trustStoreProvider) {
            this.trustStoreProvider = Optional.of(trustStoreProvider);
        }

    }

    @ConfigGroup
    public static class Proxy {

        /**
         * The host name or IP address of the Proxy.<br/>
         * Note: If the OIDC adapter requires a Proxy to talk with the OIDC server (Provider),
         * set this value to enable the usage of a Proxy.
         */
        @ConfigItem
        public Optional<String> host = Optional.empty();

        /**
         * The port number of the Proxy. The default value is `80`.
         */
        @ConfigItem(defaultValue = "80")
        public int port = 80;

        /**
         * The username, if the Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> username = Optional.empty();

        /**
         * The password, if the Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> password = Optional.empty();

    }

    public Optional<Duration> getConnectionDelay() {
        return connectionDelay;
    }

    public void setConnectionDelay(Duration connectionDelay) {
        this.connectionDelay = Optional.of(connectionDelay);
    }

    public Optional<String> getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = Optional.of(authServerUrl);
    }

    public Optional<String> getRegistrationPath() {
        return registrationPath;
    }

    public void setRegistrationPath(String registrationPath) {
        this.registrationPath = Optional.of(registrationPath);
    }

    public Optional<Boolean> isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = Optional.of(enabled);
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public OptionalInt getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = OptionalInt.of(maxPoolSize);
    }

    public Optional<Boolean> getDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(Boolean discoveryEnabled) {
        this.discoveryEnabled = Optional.of(discoveryEnabled);
    }
}

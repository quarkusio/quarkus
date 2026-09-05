package io.quarkus.oidc.common.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class OidcCommonConfig implements io.quarkus.oidc.common.runtime.config.OidcCommonConfig {

    public OidcCommonConfig() {

    }

    protected OidcCommonConfig(io.quarkus.oidc.common.runtime.config.OidcCommonConfig mapping) {
        this.authServerUrl = mapping.authServerUrl();
        this.discoveryPath = mapping.discoveryPath();
        this.discoveryEnabled = mapping.discoveryEnabled();
        this.registrationPath = mapping.registrationPath();
        this.connectionDelay = mapping.connectionDelay();
        this.connectionRetryCount = mapping.connectionRetryCount();
        this.connectionTimeout = mapping.connectionTimeout();
        this.useBlockingDnsLookup = mapping.useBlockingDnsLookup();
        this.maxPoolSize = mapping.maxPoolSize();
        this.followRedirects = mapping.followRedirects();
        this.proxy.addConfigMappingValues(mapping.proxy());
        this.tls.addConfigMappingValues(mapping.tls());
    }

    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * Do not set this property if you use 'quarkus-oidc' and the public key verification ({@link #publicKey})
     * or certificate chain verification only ({@link #certificateChain}) is required.
     * The OIDC discovery endpoint is called by default by appending a `.well-known/openid-configuration` path to this URL.
     * For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.
     *
     * @deprecated use {@link #authServerUrl()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Discovery of the OIDC endpoints.
     * If not enabled, you must configure the OIDC endpoint URLs individually.
     *
     * @deprecated use {@link #discoveryEnabled()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Boolean> discoveryEnabled = Optional.empty();

    /**
     * The relative path of the OIDC discovery endpoint.
     */
    private String discoveryPath;

    /**
     * The relative path or absolute URL of the OIDC dynamic client registration endpoint.
     * Set if {@link #discoveryEnabled} is `false` or a discovered token endpoint path must be customized.
     *
     * @deprecated use {@link #registrationPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> registrationPath = Optional.empty();

    /**
     * The duration to attempt the initial connection to an OIDC server.
     * For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart.
     * This property is only effective when the initial OIDC connection is created.
     * For dropped connections, use the `connection-retry-count` property instead.
     *
     * @deprecated use {@link #connectionDelay()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost.
     * Different from `connection-delay`, which applies only to initial connection attempts.
     * For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this
     * setting.
     *
     * @deprecated use {@link #connectionRetryCount()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public int connectionRetryCount = 3;

    /**
     * The number of seconds after which the current OIDC connection request times out.
     *
     * @deprecated use {@link #connectionTimeout()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Whether DNS lookup should be performed on the worker thread.
     * Use this option when you can see logged warnings about blocked Vert.x event loop by HTTP requests to OIDC server.
     *
     * @deprecated use {@link #useBlockingDnsLookup()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean useBlockingDnsLookup;

    /**
     * The maximum size of the connection pool used by the WebClient.
     *
     * @deprecated use {@link #maxPoolSize()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public OptionalInt maxPoolSize = OptionalInt.empty();

    /**
     * Follow redirects automatically when WebClient gets HTTP 302.
     * When this property is disabled only a single redirect to exactly the same original URI
     * is allowed but only if one or more cookies were set during the redirect request.
     *
     * @deprecated use {@link #followRedirects()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean followRedirects = true;

    /**
     * Options to configure the proxy the OIDC adapter uses to talk with the OIDC server.
     *
     * @deprecated use {@link #proxy()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Proxy proxy = new Proxy();

    /**
     * TLS configurations
     *
     * @deprecated use {@link #tls()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Tls tls = new Tls();

    @Override
    public Optional<String> authServerUrl() {
        return authServerUrl;
    }

    @Override
    public String discoveryPath() {
        return discoveryPath;
    }

    @Override
    public Optional<Boolean> discoveryEnabled() {
        return discoveryEnabled;
    }

    @Override
    public Optional<String> registrationPath() {
        return registrationPath;
    }

    @Override
    public Optional<Duration> connectionDelay() {
        return connectionDelay;
    }

    @Override
    public int connectionRetryCount() {
        return connectionRetryCount;
    }

    @Override
    public Duration connectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public boolean useBlockingDnsLookup() {
        return useBlockingDnsLookup;
    }

    @Override
    public OptionalInt maxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public boolean followRedirects() {
        return followRedirects;
    }

    @Override
    public io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Proxy proxy() {
        return proxy;
    }

    @Override
    public io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Tls tls() {
        return tls;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder} to create the TLS config
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Tls implements io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Tls {

        /**
         * The name of the TLS configuration to use.
         * <p>
         * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
         * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
         * <p>
         * The default TLS configuration is <strong>not</strong> used by default.
         */
        Optional<String> tlsConfigurationName = Optional.empty();

        private void addConfigMappingValues(io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Tls mapping) {
            this.tlsConfigurationName = mapping.tlsConfigurationName();
        }

        @Override
        public Optional<String> tlsConfigurationName() {
            return tlsConfigurationName;
        }
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder} to create the Proxy config
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Proxy implements io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Proxy {

        private Optional<String> proxyConfigurationName = Optional.empty();

        private void addConfigMappingValues(io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Proxy mapping) {
            this.proxyConfigurationName = mapping.proxyConfigurationName();
        }

        @Override
        public Optional<String> proxyConfigurationName() {
            return proxyConfigurationName;
        }
    }

    /**
     * @deprecated use the {@link #connectionDelay()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Duration> getConnectionDelay() {
        return connectionDelay;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setConnectionDelay(Duration connectionDelay) {
        this.connectionDelay = Optional.of(connectionDelay);
    }

    /**
     * @deprecated use the {@link #authServerUrl()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getAuthServerUrl() {
        return authServerUrl;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = Optional.of(authServerUrl);
    }

    /**
     * @deprecated use the {@link #registrationPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getRegistrationPath() {
        return registrationPath;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setRegistrationPath(String registrationPath) {
        this.registrationPath = Optional.of(registrationPath);
    }

    /**
     * @deprecated use the {@link #discoveryEnabled()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Boolean> isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = Optional.of(enabled);
    }

    /**
     * @deprecated use the {@link #proxy()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * @deprecated use the {@link #connectionTimeout()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @deprecated use the {@link #maxPoolSize()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public OptionalInt getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = OptionalInt.of(maxPoolSize);
    }

    /**
     * @deprecated use the {@link #discoveryEnabled()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Boolean> getDiscoveryEnabled() {
        return discoveryEnabled;
    }

    /**
     * @deprecated use {@link io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setDiscoveryEnabled(Boolean discoveryEnabled) {
        this.discoveryEnabled = Optional.of(discoveryEnabled);
    }

}

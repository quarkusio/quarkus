package io.quarkus.oidc.common.runtime.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class OidcCommonConfigBuilder<T> {

    private record TlsImpl(Optional<String> tlsConfigurationName, Optional<Verification> verification,
            Optional<Path> keyStoreFile, Optional<String> keyStoreFileType, Optional<String> keyStoreProvider,
            Optional<String> keyStorePassword, Optional<String> keyStoreKeyAlias, Optional<String> keyStoreKeyPassword,
            Optional<Path> trustStoreFile, Optional<String> trustStorePassword, Optional<String> trustStoreCertAlias,
            Optional<String> trustStoreFileType, Optional<String> trustStoreProvider) implements OidcCommonConfig.Tls {
        private TlsImpl(String tlsConfigurationName) {
            this(Optional.ofNullable(tlsConfigurationName), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    private record ProxyImpl(Optional<String> host, int port, Optional<String> username,
            Optional<String> password) implements OidcCommonConfig.Proxy {
    }

    protected static class OidcCommonConfigImpl implements OidcCommonConfig {

        private final Optional<String> authServerUrl;
        private final Optional<Boolean> discoveryEnabled;
        private final Optional<String> registrationPath;
        private final Optional<Duration> connectionDelay;
        private final int connectionRetryCount;
        private final Duration connectionTimeout;
        private final boolean useBlockingDnsLookup;
        private final OptionalInt maxPoolSize;
        private final boolean followRedirects;
        private final Proxy proxy;
        private final Tls tls;

        protected OidcCommonConfigImpl(OidcCommonConfigBuilder<?> builder) {
            this.authServerUrl = builder.authServerUrl;
            this.discoveryEnabled = builder.discoveryEnabled;
            this.registrationPath = builder.registrationPath;
            this.connectionDelay = builder.connectionDelay;
            this.connectionRetryCount = builder.connectionRetryCount;
            this.connectionTimeout = builder.connectionTimeout;
            this.useBlockingDnsLookup = builder.useBlockingDnsLookup;
            this.maxPoolSize = builder.maxPoolSize;
            this.followRedirects = builder.followRedirects;
            this.proxy = new ProxyImpl(builder.proxyHost, builder.proxyPort, builder.proxyUsername, builder.proxyPassword);
            this.tls = builder.tls;
        }

        @Override
        public Optional<String> authServerUrl() {
            return authServerUrl;
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
        public Proxy proxy() {
            return proxy;
        }

        @Override
        public Tls tls() {
            return tls;
        }
    }

    private Optional<String> authServerUrl;
    private Optional<Boolean> discoveryEnabled;
    private Optional<String> registrationPath;
    private Optional<Duration> connectionDelay;
    private int connectionRetryCount;
    private Duration connectionTimeout;
    private boolean useBlockingDnsLookup;
    private OptionalInt maxPoolSize;
    private boolean followRedirects;
    private Optional<String> proxyHost;
    private int proxyPort;
    private Optional<String> proxyUsername;
    private Optional<String> proxyPassword;
    private OidcCommonConfig.Tls tls;

    protected OidcCommonConfigBuilder(OidcCommonConfig oidcCommonConfig) {
        this.authServerUrl = oidcCommonConfig.authServerUrl();
        this.discoveryEnabled = oidcCommonConfig.discoveryEnabled();
        this.registrationPath = oidcCommonConfig.registrationPath();
        this.connectionDelay = oidcCommonConfig.connectionDelay();
        this.connectionRetryCount = oidcCommonConfig.connectionRetryCount();
        this.connectionTimeout = oidcCommonConfig.connectionTimeout();
        this.useBlockingDnsLookup = oidcCommonConfig.useBlockingDnsLookup();
        this.maxPoolSize = oidcCommonConfig.maxPoolSize();
        this.followRedirects = oidcCommonConfig.followRedirects();
        this.proxyHost = oidcCommonConfig.proxy().host();
        this.proxyPort = oidcCommonConfig.proxy().port();
        this.proxyUsername = oidcCommonConfig.proxy().username();
        this.proxyPassword = oidcCommonConfig.proxy().password();
        this.tls = oidcCommonConfig.tls();
    }

    protected abstract T getBuilder();

    /**
     * @param authServerUrl {@link OidcCommonConfig#authServerUrl()}
     * @return T builder
     */
    public T authServerUrl(String authServerUrl) {
        this.authServerUrl = Optional.ofNullable(authServerUrl);
        return getBuilder();
    }

    /**
     * @param discoveryEnabled {@link OidcCommonConfig#discoveryEnabled()}
     * @return T builder
     */
    public T discoveryEnabled(boolean discoveryEnabled) {
        this.discoveryEnabled = Optional.of(discoveryEnabled);
        return getBuilder();
    }

    /**
     * @param registrationPath {@link OidcCommonConfig#registrationPath()}
     * @return T builder
     */
    public T registrationPath(String registrationPath) {
        this.registrationPath = Optional.ofNullable(registrationPath);
        return getBuilder();
    }

    /**
     * @param connectionDelay {@link OidcCommonConfig#connectionDelay()}
     * @return T builder
     */
    public T connectionDelay(Duration connectionDelay) {
        this.connectionDelay = Optional.ofNullable(connectionDelay);
        return getBuilder();
    }

    /**
     * @param connectionRetryCount {@link OidcCommonConfig#connectionRetryCount()}
     * @return T builder
     */
    public T connectionRetryCount(int connectionRetryCount) {
        this.connectionRetryCount = connectionRetryCount;
        return getBuilder();
    }

    /**
     * @param connectionTimeout {@link OidcCommonConfig#connectionTimeout()}
     * @return T builder
     */
    public T connectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return getBuilder();
    }

    /**
     * @param useBlockingDnsLookup {@link OidcCommonConfig#useBlockingDnsLookup()}
     * @return T builder
     */
    public T useBlockingDnsLookup(boolean useBlockingDnsLookup) {
        this.useBlockingDnsLookup = useBlockingDnsLookup;
        return getBuilder();
    }

    /**
     * @param maxPoolSize {@link OidcCommonConfig#maxPoolSize()}
     * @return T builder
     */
    public T maxPoolSize(int maxPoolSize) {
        this.maxPoolSize = OptionalInt.of(maxPoolSize);
        return getBuilder();
    }

    /**
     * @param followRedirects {@link OidcCommonConfig#followRedirects()}
     * @return T builder
     */
    public T followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return getBuilder();
    }

    /**
     * @param host {@link OidcCommonConfig.Proxy#host()}
     * @param port {@link OidcCommonConfig.Proxy#port()}
     * @return T builder
     */
    public T proxy(String host, int port) {
        this.proxyHost = Optional.ofNullable(host);
        this.proxyPort = port;
        return getBuilder();
    }

    /**
     * @param host {@link OidcCommonConfig.Proxy#host()}
     * @param port {@link OidcCommonConfig.Proxy#port()}
     * @param username {@link OidcCommonConfig.Proxy#username()}
     * @param password {@link OidcCommonConfig.Proxy#password()}
     * @return T builder
     */
    public T proxy(String host, int port, String username, String password) {
        this.proxyHost = Optional.ofNullable(host);
        this.proxyPort = port;
        this.proxyUsername = Optional.ofNullable(username);
        this.proxyPassword = Optional.ofNullable(password);
        return getBuilder();
    }

    /**
     * @param tlsConfigurationName {@link OidcCommonConfig.Tls#tlsConfigurationName()}
     * @return T builder
     */
    public T tlsConfigurationName(String tlsConfigurationName) {
        this.tls = new TlsImpl(tlsConfigurationName);
        return getBuilder();
    }
}

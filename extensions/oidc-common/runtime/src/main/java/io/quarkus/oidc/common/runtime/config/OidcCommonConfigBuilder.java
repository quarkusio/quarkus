package io.quarkus.oidc.common.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class OidcCommonConfigBuilder<T> {

    private record TlsImpl(Optional<String> tlsConfigurationName) implements OidcCommonConfig.Tls {
        private TlsImpl(String tlsConfigurationName) {
            this(Optional.ofNullable(tlsConfigurationName));
        }
    }

    private record ProxyImpl(Optional<String> proxyConfigurationName) implements OidcCommonConfig.Proxy {
    }

    protected static class OidcCommonConfigImpl implements OidcCommonConfig {

        private final Optional<String> authServerUrl;
        private final String discoveryPath;
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
            this.discoveryPath = builder.discoveryPath;
            this.discoveryEnabled = builder.discoveryEnabled;
            this.registrationPath = builder.registrationPath;
            this.connectionDelay = builder.connectionDelay;
            this.connectionRetryCount = builder.connectionRetryCount;
            this.connectionTimeout = builder.connectionTimeout;
            this.useBlockingDnsLookup = builder.useBlockingDnsLookup;
            this.maxPoolSize = builder.maxPoolSize;
            this.followRedirects = builder.followRedirects;
            this.proxy = new ProxyImpl(builder.proxyConfigurationName);
            this.tls = builder.tls;
        }

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
        public Proxy proxy() {
            return proxy;
        }

        @Override
        public Tls tls() {
            return tls;
        }
    }

    private Optional<String> authServerUrl;
    private String discoveryPath;
    private Optional<Boolean> discoveryEnabled;
    private Optional<String> registrationPath;
    private Optional<Duration> connectionDelay;
    private int connectionRetryCount;
    private Duration connectionTimeout;
    private boolean useBlockingDnsLookup;
    private OptionalInt maxPoolSize;
    private boolean followRedirects;
    private Optional<String> proxyConfigurationName;
    private OidcCommonConfig.Tls tls;

    protected OidcCommonConfigBuilder(OidcCommonConfig oidcCommonConfig) {
        this.authServerUrl = oidcCommonConfig.authServerUrl();
        this.discoveryPath = oidcCommonConfig.discoveryPath();
        this.discoveryEnabled = oidcCommonConfig.discoveryEnabled();
        this.registrationPath = oidcCommonConfig.registrationPath();
        this.connectionDelay = oidcCommonConfig.connectionDelay();
        this.connectionRetryCount = oidcCommonConfig.connectionRetryCount();
        this.connectionTimeout = oidcCommonConfig.connectionTimeout();
        this.useBlockingDnsLookup = oidcCommonConfig.useBlockingDnsLookup();
        this.maxPoolSize = oidcCommonConfig.maxPoolSize();
        this.followRedirects = oidcCommonConfig.followRedirects();
        this.proxyConfigurationName = oidcCommonConfig.proxy().proxyConfigurationName();
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
     * @param discoveryPath {@link OidcCommonConfig#discoveryPath()}
     * @return T builder
     */
    public T discoveryPath(String discoveryPath) {
        this.discoveryPath = discoveryPath;
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
     * @param tlsConfigurationName {@link OidcCommonConfig.Tls#tlsConfigurationName()}
     * @return T builder
     */
    public T tlsConfigurationName(String tlsConfigurationName) {
        this.tls = new TlsImpl(tlsConfigurationName);
        return getBuilder();
    }

    /**
     * @param proxyConfigurationName {@link OidcCommonConfig.Proxy#proxyConfigurationName()}
     * @return T builder
     */
    public T proxyConfigurationName(String proxyConfigurationName) {
        this.proxyConfigurationName = Optional.ofNullable(proxyConfigurationName);
        return getBuilder();
    }
}

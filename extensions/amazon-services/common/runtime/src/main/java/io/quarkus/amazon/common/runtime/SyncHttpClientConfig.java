package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SyncHttpClientConfig {
    /**
     * The maximum amount of time to establish a connection before timing out.
     */
    @ConfigItem(defaultValue = "2S")
    public Duration connectionTimeout;

    /**
     * The amount of time to wait for data to be transferred over an established, open connection before the connection is timed
     * out.
     */
    @ConfigItem(defaultValue = "30S")
    public Duration socketTimeout;

    /**
     * TLS Key Managers provider configuration
     */
    @ConfigItem
    public TlsKeyManagersProviderConfig tlsKeyManagersProvider;

    /**
     * TLS Trust Managers provider configuration
     */
    @ConfigItem
    public TlsTrustManagersProviderConfig tlsTrustManagersProvider;

    /**
     * Apache HTTP client specific configurations
     */
    @ConfigItem
    @ConfigDocSection
    public ApacheHttpClientConfig apache;

    @ConfigGroup
    public static class ApacheHttpClientConfig {
        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         */
        @ConfigItem(defaultValue = "10S")
        public Duration connectionAcquisitionTimeout;

        /**
         * The maximum amount of time that a connection should be allowed to remain open while idle.
         */
        @ConfigItem(defaultValue = "60S")
        public Duration connectionMaxIdleTime;

        /**
         * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         */
        @ConfigItem
        public Optional<Duration> connectionTimeToLive;

        /**
         * The maximum number of connections allowed in the connection pool.
         * <p>
         * Each built HTTP client has its own private connection pool.
         */
        @ConfigItem(defaultValue = "50")
        public int maxConnections;

        /**
         * Whether the client should send an HTTP expect-continue handshake before each request.
         */
        @ConfigItem(defaultValue = "true")
        public boolean expectContinueEnabled;

        /**
         * Whether the idle connections in the connection pool should be closed asynchronously.
         * <p>
         * When enabled, connections left idling for longer than `quarkus.<amazon-service>.sync-client.connection-max-idle-time`
         * will be closed.
         * This will not close connections currently in use.
         */
        @ConfigItem(defaultValue = "true")
        public boolean useIdleConnectionReaper;

        /**
         * HTTP proxy configuration
         */
        @ConfigItem
        public HttpClientProxyConfiguration proxy;

        @ConfigGroup
        public static class HttpClientProxyConfiguration {

            /**
             * Enable HTTP proxy
             */
            @ConfigItem
            public boolean enabled;

            /**
             * The endpoint of the proxy server that the SDK should connect through.
             * <p>
             * Currently, the endpoint is limited to a host and port. Any other URI components will result in an exception being
             * raised.
             */
            @ConfigItem
            public Optional<URI> endpoint;

            /**
             * The username to use when connecting through a proxy.
             */
            @ConfigItem
            public Optional<String> username;

            /**
             * The password to use when connecting through a proxy.
             */
            @ConfigItem
            public Optional<String> password;

            /**
             * For NTLM proxies - the Windows domain name to use when authenticating with the proxy.
             */
            @ConfigItem
            public Optional<String> ntlmDomain;

            /**
             * For NTLM proxies - the Windows workstation name to use when authenticating with the proxy.
             */
            @ConfigItem
            public Optional<String> ntlmWorkstation;

            /**
             * Whether to attempt to authenticate preemptively against the proxy server using basic authentication.
             */
            @ConfigItem
            public Optional<Boolean> preemptiveBasicAuthenticationEnabled;

            /**
             * The hosts that the client is allowed to access without going through the proxy.
             */
            @ConfigItem
            public Optional<List<String>> nonProxyHosts;
        }
    }
}

package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ApacheHttpClientConfig {

    /**
     * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
     *
     * <p>
     * Default is 10 seconds.
     */
    @ConfigItem
    public Optional<Duration> connectionAcquisitionTimeout;

    /**
     * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
     *
     * <p>
     * Default is 60 seconds.
     */
    @ConfigItem
    public Optional<Duration> connectionMaxIdleTime;

    /**
     * Configure the endpoint with which the SDK should communicate.
     *
     * <p>
     * If not specified, an appropriate endpoint to be used for a given service and region.
     * <p>
     * Default is 2 seconds.
     */
    @ConfigItem
    public Optional<Duration> connectionTimeout;

    /**
     * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
     */
    @ConfigItem
    public Optional<Duration> connectionTimeToLive;

    /**
     * The amount of time to wait for data to be transferred over an established, open connection before the connection is timed
     * out.
     *
     * <p>
     * A duration of 0 means infinity, and is not recommended.
     */
    @ConfigItem
    public Optional<Duration> socketTimeout;

    /**
     * The maximum number of connections allowed in the connection pool.
     *
     * <p>
     * Each built HTTP client has its own private connection pool.
     */
    @ConfigItem
    public OptionalInt maxConnections;

    /**
     * Configure whether the client should send an HTTP expect-continue handshake before each request.
     *
     * <p>
     * By default, this is enabled.
     */
    @ConfigItem
    public Optional<Boolean> expectContinueEnabled;

    /**
     * Configure whether the idle connections in the connection pool should be closed asynchronously.
     *
     * <p>
     * When enabled, connections left idling for longer than `quarkus.dynamodb.sync-client.connection-max-idle-time` will be
     * closed.
     * This will not close connections currently in use.
     *
     * <p>
     * By default, this is enabled.
     */
    @ConfigItem
    public Optional<Boolean> useIdleConnectionReaper;

    /**
     * HTTP proxy configuration
     */
    @ConfigItem
    public HttpClientProxyConfiguration proxy;

    /**
     * TLS Managers provider configuration
     */
    @ConfigItem
    public TlsManagersProviderConfig tlsManagersProvider;

    @ConfigGroup
    public static class HttpClientProxyConfiguration {

        /**
         * Enable HTTP proxy
         */
        @ConfigItem
        public boolean enabled;

        /**
         * Configure the endpoint of the proxy server that the SDK should connect through.
         *
         * <p>
         * Currently, the endpoint is limited to a host and port. Any other URI components will result in an exception being
         * raised.
         */
        @ConfigItem
        public URI endpoint;

        /**
         * Configure the username to use when connecting through a proxy.
         */
        @ConfigItem
        public Optional<String> username;

        /**
         * Configure the password to use when connecting through a proxy.
         */
        @ConfigItem
        public Optional<String> password;

        /**
         * For NTLM proxies - configure the Windows domain name to use when authenticating with the proxy.
         */
        @ConfigItem
        public Optional<String> ntlmDomain;

        /**
         * For NTLM proxies - configure the Windows workstation name to use when authenticating with the proxy.
         */
        @ConfigItem
        public Optional<String> ntlmWorkstation;

        /**
         * Configure whether to attempt to authenticate pre-emptively against the proxy server using basic authentication.
         */
        @ConfigItem
        public Optional<Boolean> preemptiveBasicAuthenticationEnabled;

        /**
         * Configure the hosts that the client is allowed to access without going through the proxy.
         */
        @ConfigItem
        public List<String> nonProxyHosts;
    }
}

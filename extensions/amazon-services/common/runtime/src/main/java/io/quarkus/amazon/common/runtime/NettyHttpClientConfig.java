package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.netty.handler.ssl.SslProvider;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import software.amazon.awssdk.http.Protocol;

@ConfigGroup
public class NettyHttpClientConfig {

    /**
     * The maximum number of allowed concurrent requests.
     * <p>
     * For HTTP/1.1 this is the same as max connections. For HTTP/2 the number of connections that will be used depends on the
     * max streams allowed per connection.
     */
    @ConfigItem(defaultValue = "50")
    public int maxConcurrency;

    /**
     * The maximum number of pending acquires allowed.
     * <p>
     * Once this exceeds, acquire tries will be failed.
     */
    @ConfigItem(defaultValue = "10000")
    public int maxPendingConnectionAcquires;

    /**
     * The amount of time to wait for a read on a socket before an exception is thrown.
     * <p>
     * Specify `0` to disable.
     */
    @ConfigItem(defaultValue = "30S")
    public Duration readTimeout;

    /**
     * The amount of time to wait for a write on a socket before an exception is thrown.
     * <p>
     * Specify `0` to disable.
     */
    @ConfigItem(defaultValue = "30S")
    public Duration writeTimeout;

    /**
     * The amount of time to wait when initially establishing a connection before giving up and timing out.
     */
    @ConfigItem(defaultValue = "10S")
    public Duration connectionTimeout;

    /**
     * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
     */
    @ConfigItem(defaultValue = "2S")
    public Duration connectionAcquisitionTimeout;

    /**
     * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
     */
    @ConfigItem
    public Optional<Duration> connectionTimeToLive;

    /**
     * The maximum amount of time that a connection should be allowed to remain open while idle.
     * <p>
     * Currently has no effect if `quarkus.dynamodb.async-client.use-idle-connection-reaper` is false.
     */
    @ConfigItem(defaultValue = "60S")
    public Duration connectionMaxIdleTime;

    /**
     * Whether the idle connections in the connection pool should be closed.
     * <p>
     * When enabled, connections left idling for longer than `quarkus.dynamodb.async-client.connection-max-idle-time` will be
     * closed. This will not close connections currently in use.
     */
    @ConfigItem(defaultValue = "true")
    public boolean useIdleConnectionReaper;

    /**
     * The HTTP protocol to use.
     */
    @ConfigItem(defaultValue = "http1-1")
    public Protocol protocol;

    /**
     * The SSL Provider to be used in the Netty client.
     * <p>
     * Default is `OPENSSL` if available, `JDK` otherwise.
     */
    @ConfigItem
    public Optional<SslProvider> sslProvider;

    /**
     * HTTP/2 specific configuration
     */
    @ConfigItem
    public Http2Config http2;

    /**
     * HTTP proxy configuration
     */
    @ConfigItem
    public NettyProxyConfiguration proxy;

    /**
     * TLS Managers provider configuration
     */
    @ConfigItem
    public TlsManagersProviderConfig tlsManagersProvider;

    /**
     * Netty event loop configuration override
     */
    @ConfigItem
    public SdkEventLoopGroupConfig eventLoop;

    @ConfigGroup
    public static class Http2Config {
        /**
         * The maximum number of concurrent streams for an HTTP/2 connection.
         * <p>
         * This setting is only respected when the HTTP/2 protocol is used.
         * <p>
         */
        @ConfigItem(defaultValueDocumentation = "4294967295")
        public Optional<Long> maxStreams;

        /**
         * The initial window size for an HTTP/2 stream.
         * <p>
         * This setting is only respected when the HTTP/2 protocol is used.
         * <p>
         */
        @ConfigItem(defaultValueDocumentation = "1048576")
        public Optional<Integer> initialWindowSize;
    }

    @ConfigGroup
    public static class SdkEventLoopGroupConfig {

        /**
         * Enable the custom configuration of the Netty event loop group.
         */
        @ConfigItem
        public boolean override;

        /**
         * Number of threads to use for the event loop group.
         * <p>
         * If not set, the default Netty thread count is used (which is double the number of available processors unless the
         * `io.netty.eventLoopThreads` system property is set.
         */
        @ConfigItem
        public Optional<Integer> numberOfThreads;

        /**
         * The thread name prefix for threads created by this thread factory used by event loop group.
         * <p>
         * The prefix will be appended with a number unique to the thread factory and a number unique to the thread.
         * <p>
         * If not specified it defaults to `aws-java-sdk-NettyEventLoop`
         */
        @ConfigItem
        public Optional<String> threadNamePrefix;
    }

    @ConfigGroup
    public static class NettyProxyConfiguration {

        /**
         * Enable HTTP proxy.
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
         * The hosts that the client is allowed to access without going through the proxy.
         */
        @ConfigItem
        public Optional<List<String>> nonProxyHosts;
    }

    //TODO: additionalChannelOptions
    //additionalChannelOptions;
}

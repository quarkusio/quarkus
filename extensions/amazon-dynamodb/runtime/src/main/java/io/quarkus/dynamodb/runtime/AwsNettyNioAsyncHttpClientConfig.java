package io.quarkus.dynamodb.runtime;

import java.time.Duration;
import java.util.Optional;

import io.netty.handler.ssl.SslProvider;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import software.amazon.awssdk.http.Protocol;

@ConfigGroup
public class AwsNettyNioAsyncHttpClientConfig {

    /**
     * Max concurrency
     */
    @ConfigItem
    public Optional<Integer> maxConcurrency;

    /**
     * Max pending connection acquires
     */
    @ConfigItem
    public Optional<Integer> maxPendingConnectionAcquires;

    /**
     * Read timeout
     */
    @ConfigItem
    public Optional<Duration> readTimeout;

    /**
     * Write timeout
     */
    @ConfigItem
    public Optional<Duration> writeTimeout;

    /**
     * Connection timeout
     */
    @ConfigItem
    public Optional<Duration> connectionTimeout;

    /**
     * Connection acquisition timeout
     */
    @ConfigItem
    public Optional<Duration> connectionAcquisitionTimeout;

    /**
     * Connection time to live
     */
    @ConfigItem
    public Optional<Duration> connectionTimeToLive;

    /**
     * Connection max idle time
     */
    @ConfigItem
    public Optional<Duration> connectionMaxIdleTime;

    /**
     * Use idle connection reaper
     */
    @ConfigItem
    public Optional<Boolean> useIdleConnectionReaper;

    /**
     * HTTP protocol
     */
    @ConfigItem
    public Optional<Protocol> protocol;

    /**
     * Max Http2 streams
     */
    @ConfigItem
    public Optional<Integer> maxHttp2Streams;

    /**
     * SSL provider
     */
    @ConfigItem
    public Optional<SslProvider> sslProvider;

    /**
     * Netty event loop configuration override
     */
    @ConfigItem
    public SdkEventLoopGroupConfig eventLoop;

    @ConfigGroup
    public static class SdkEventLoopGroupConfig {

        /**
         * Enables overrides for event loop
         */
        @ConfigItem(defaultValue = "false")
        public boolean override;

        /**
         * Defines amount of threads for event loop
         */
        @ConfigItem
        public Integer numberOfThreads;

        /**
         * Defines event loop thread name prefix
         */
        @ConfigItem
        public Optional<String> threadNamePrefix;
    }

    //TODO: additionalChannelOptions
    //additionalChannelOptions;
}

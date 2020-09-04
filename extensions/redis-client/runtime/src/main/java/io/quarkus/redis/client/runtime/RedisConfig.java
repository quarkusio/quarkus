package io.quarkus.redis.client.runtime;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.redis.client.RedisClientType;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class RedisConfig {

    /**
     * The redis password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The redis hosts
     */
    @ConfigItem(defaultValue = "localhost:6379")
    public Optional<Set<InetSocketAddress>> hosts;

    /**
     * The redis database
     */
    @ConfigItem
    public int database;

    /**
     * The maximum delay to wait before a blocking command to redis server times out
     */
    @ConfigItem(defaultValue = "10s")
    public Optional<Duration> timeout;

    /**
     * Enables or disables the SSL on connect.
     */
    @ConfigItem
    public boolean ssl;

    /**
     * The redis client type
     */
    @ConfigItem(defaultValue = "standalone")
    public RedisClientType clientType;

    /**
     * The maximum size of the connection pool. When working with cluster or sentinel.
     * <p>
     * This value should be at least the total number of cluster member (or number of sentinels + 1)
     */
    @ConfigItem(defaultValue = "6")
    public int maxPoolSize;

    /**
     * The maximum waiting requests for a connection from the pool.
     */
    @ConfigItem(defaultValue = "24")
    public int maxPoolWaiting;

    /**
     * The duration indicating how often should the connection pool cleaner executes.
     */
    @ConfigItem
    public Optional<Duration> poolCleanerInterval;

    /**
     * The timeout for a connection recycling.
     */
    @ConfigItem(defaultValue = "15")
    public Duration poolRecycleTimeout;

    /**
     * Sets how much handlers is the client willing to queue.
     * <p>
     * The client will always work on pipeline mode, this means that messages can start queueing.
     * Using this configuration option, you can control how much backlog you're willing to accept.
     */
    @ConfigItem(defaultValue = "2048")
    public int maxWaitingHandlers;

    /**
     * Tune how much nested arrays are allowed on a redis response. This affects the parser performance.
     */
    @ConfigItem(defaultValue = "32")
    public int maxNestedArrays;
}

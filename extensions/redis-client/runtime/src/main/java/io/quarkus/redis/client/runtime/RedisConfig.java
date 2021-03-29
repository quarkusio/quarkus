package io.quarkus.redis.client.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisRole;
import io.vertx.redis.client.RedisSlaves;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class RedisConfig {

    /**
     * The default redis client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public RedisConfiguration defaultClient;

    /**
     * Configures additional Redis client connections.
     * <p>
     * Each client has a unique identifier which must be identified to select the right connection.
     * For example:
     * <p>
     *
     * <pre>
     * quarkus.redis.client1.hosts = redis://localhost:6379
     * quarkus.redis.client2.hosts = redis://localhost:6380
     * </pre>
     * <p>
     * And then use the {@link RedisClientName} annotation to select the {@link RedisClient} or
     * {@link io.quarkus.redis.client.reactive.ReactiveRedisClient}.
     * <p>
     * 
     * <pre>
     * {@code
     * &#64;RedisClientName("client1")
     * &#64;Inject
     * RedisClient redisClient1
     * }
     * </pre>
     */
    @ConfigItem(name = ConfigItem.PARENT)
    Map<String, RedisConfiguration> additionalRedisClients;

    @ConfigGroup
    public static class RedisConfiguration {
        /**
         * The redis hosts to use while connecting to the redis server. Only the cluster mode will consider more than
         * 1 element.
         * <p>
         * The URI provided uses the following schema `redis://[username:password@][host][:port][/database]`
         * 
         * @see <a href="https://www.iana.org/assignments/uri-schemes/prov/redis">Redis scheme on www.iana.org</a>
         */
        @ConfigItem(defaultValueDocumentation = "redis://localhost:6379")
        public Optional<Set<URI>> hosts;

        /**
         * The maximum delay to wait before a blocking command to redis server times out
         */
        @ConfigItem(defaultValue = "10s")
        public Optional<Duration> timeout;

        /**
         * The redis client type
         */
        @ConfigItem(defaultValue = "standalone")
        public RedisClientType clientType;

        /**
         * The master name (only considered in HA mode).
         */
        @ConfigItem(defaultValueDocumentation = "mymaster")
        public Optional<String> masterName;

        /**
         * The role name (only considered in HA mode).
         */
        @ConfigItem(defaultValueDocumentation = "master")
        public Optional<RedisRole> role;

        /**
         * Whether or not to use slave nodes (only considered in Cluster mode).
         */
        @ConfigItem(defaultValueDocumentation = "never")
        public Optional<RedisSlaves> slaves;

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
}

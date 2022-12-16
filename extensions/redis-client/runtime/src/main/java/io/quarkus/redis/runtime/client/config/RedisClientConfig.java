package io.quarkus.redis.runtime.client.config;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class RedisClientConfig {
    /**
     * The redis hosts to use while connecting to the redis server. Only the cluster and sentinel modes will consider more than
     * 1 element.
     * <p>
     * The URI provided uses the following schema `redis://[username:password@][host][:port][/database]`
     * Use `quarkus.redis.hosts-provider-name` to provide the hosts programmatically.
     * <p>
     *
     * @see <a href="https://www.iana.org/assignments/uri-schemes/prov/redis">Redis scheme on www.iana.org</a>
     */
    @ConfigItem(name = RedisConfig.HOSTS_CONFIG_NAME)
    public Optional<Set<URI>> hosts;

    /**
     * The hosts provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the hosts provider bean. It is used to discriminate if multiple
     * `io.quarkus.redis.client.RedisHostsProvider` beans are available.
     *
     * <p>
     * Used when `quarkus.redis.hosts` is not set.
     */
    @ConfigItem
    public Optional<String> hostsProviderName;

    /**
     * The maximum delay to wait before a blocking command to redis server times out
     */
    @ConfigItem(defaultValue = "10s")
    public Duration timeout;

    /**
     * The redis client type.
     * Accepted values are: {@code STANDALONE} (default), {@code CLUSTER}, {@code REPLICATION}, {@code SENTINEL}.
     */
    @ConfigItem(defaultValue = "standalone")
    public RedisClientType clientType;

    /**
     * The master name (only considered in HA mode).
     */
    @ConfigItem(defaultValueDocumentation = "my-master")
    public Optional<String> masterName;

    /**
     * The role name (only considered in Sentinel / HA mode).
     * Accepted values are: {@code MASTER}, {@code REPLICA}, {@code SENTINEL}.
     */
    @ConfigItem(defaultValueDocumentation = "master")
    public Optional<RedisRole> role;

    /**
     * Whether to use replicas nodes (only considered in Cluster mode).
     * Accepted values are: {@code ALWAYS}, {@code NEVER}, {@code SHARE}.
     */
    @ConfigItem(defaultValueDocumentation = "never")
    public Optional<RedisReplicas> replicas;

    /**
     * The default password for cluster/sentinel connections.
     * <p>
     * If not set it will try to extract the value from the current default {@code #hosts}.
     */
    @ConfigItem
    public Optional<String> password;

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
     * Sets how many handlers is the client willing to queue.
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

    /**
     * The number of reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "0")
    public int reconnectAttempts;

    /**
     * The interval between reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "1")
    public Duration reconnectInterval;

    /**
     * Should the client perform {@code RESP protocol negotiation during the connection handshake.
     */
    @ConfigItem(defaultValue = "true")
    public boolean protocolNegotiation;

    /**
     * TCP config.
     */
    @ConfigItem
    @ConfigDocSection
    public NetConfig tcp;

    /**
     * SSL/TLS config.
     */
    @ConfigItem
    @ConfigDocSection
    public TlsConfig tls;

    @Override
    public String toString() {
        return "RedisClientConfig{" +
                "hosts=" + hosts +
                ", hostsProviderName=" + hostsProviderName +
                ", timeout=" + timeout +
                ", clientType=" + clientType +
                ", masterName=" + masterName +
                ", role=" + role +
                ", replicas=" + replicas +
                ", password=" + password +
                ", maxPoolSize=" + maxPoolSize +
                ", maxPoolWaiting=" + maxPoolWaiting +
                ", poolCleanerInterval=" + poolCleanerInterval +
                ", poolRecycleTimeout=" + poolRecycleTimeout +
                ", maxWaitingHandlers=" + maxWaitingHandlers +
                ", maxNestedArrays=" + maxNestedArrays +
                ", reconnectAttempts=" + reconnectAttempts +
                ", reconnectInterval=" + reconnectInterval +
                ", protocolNegotiation=" + protocolNegotiation +
                ", tcp=" + tcp +
                ", tls=" + tls +
                '}';
    }

}

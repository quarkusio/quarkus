package io.quarkus.redis.runtime.client.config;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.vertx.redis.client.ProtocolVersion;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;
import io.vertx.redis.client.RedisTopology;

@ConfigGroup
public interface RedisClientConfig {

    /**
     * The Redis hosts to use while connecting to the Redis server. Only the cluster and sentinel modes will consider more than
     * 1 element.
     * <p>
     * The URI provided uses the following schema `redis://[username:password@][host][:port][/database]`
     * Use `quarkus.redis.hosts-provider-name` to provide the hosts programmatically.
     * <p>
     *
     * @see <a href="https://www.iana.org/assignments/uri-schemes/prov/redis">Redis scheme on www.iana.org</a>
     */
    Optional<Set<URI>> hosts();

    /**
     * The hosts provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the hosts provider bean. It is used to discriminate if multiple
     * `io.quarkus.redis.client.RedisHostsProvider` beans are available.
     *
     * <p>
     * Used when `quarkus.redis.hosts` is not set.
     */
    Optional<String> hostsProviderName();

    /**
     * The maximum delay to wait before a blocking command to Redis server times out
     */
    @WithDefault("10s")
    Duration timeout();

    /**
     * The Redis client type.
     * Accepted values are: {@code STANDALONE} (default), {@code CLUSTER}, {@code REPLICATION}, {@code SENTINEL}.
     */
    @WithDefault("standalone")
    RedisClientType clientType();

    /**
     * The master name (only considered in the Sentinel mode).
     */
    @ConfigDocDefault("mymaster")
    Optional<String> masterName();

    /**
     * The role name (only considered in the Sentinel mode).
     * Accepted values are: {@code MASTER}, {@code REPLICA}, {@code SENTINEL}.
     */
    @ConfigDocDefault("master")
    Optional<RedisRole> role();

    /**
     * Whether to use replicas nodes (only considered in Cluster mode and Replication mode).
     * Accepted values are: {@code ALWAYS}, {@code NEVER}, {@code SHARE}.
     */
    @ConfigDocDefault("never")
    Optional<RedisReplicas> replicas();

    /**
     * The default password for Redis connections.
     * <p>
     * If not set, it will try to extract the value from the {@code hosts}.
     */
    Optional<String> password();

    /**
     * The maximum size of the connection pool.
     */
    @WithDefault("6")
    int maxPoolSize();

    /**
     * The maximum waiting requests for a connection from the pool.
     */
    @WithDefault("24")
    int maxPoolWaiting();

    /**
     * The duration indicating how often should the connection pool cleaner execute.
     */
    @ConfigDocDefault("30s")
    Optional<Duration> poolCleanerInterval();

    /**
     * The timeout for unused connection recycling.
     */
    @WithDefault("3m")
    Optional<Duration> poolRecycleTimeout();

    /**
     * Sets how many handlers is the client willing to queue.
     * <p>
     * The client will always work on pipeline mode, this means that messages can start queueing.
     * Using this configuration option, you can control how much backlog you're willing to accept.
     */
    @WithDefault("2048")
    int maxWaitingHandlers();

    /**
     * Tune how much nested arrays are allowed on a Redis response. This affects the parser performance.
     */
    @WithDefault("32")
    int maxNestedArrays();

    /**
     * The number of reconnection attempts when a pooled connection cannot be established on first try.
     */
    @WithDefault("0")
    int reconnectAttempts();

    /**
     * The interval between reconnection attempts when a pooled connection cannot be established on first try.
     */
    @WithDefault("1")
    Duration reconnectInterval();

    /**
     * Should the client perform {@code RESP} protocol negotiation during the connection handshake.
     */
    @WithDefault("true")
    boolean protocolNegotiation();

    /**
     * The preferred protocol version to be used during protocol negotiation. When not set,
     * defaults to RESP 3. When protocol negotiation is disabled, this setting has no effect.
     */
    @ConfigDocDefault("resp3")
    Optional<ProtocolVersion> preferredProtocolVersion();

    /**
     * The TTL of the hash slot cache. A hash slot cache is used by the clustered Redis client
     * to prevent constantly sending {@code CLUSTER SLOTS} commands to the first statically
     * configured cluster node.
     * <p>
     * This setting is only meaningful in case of a clustered Redis client and has no effect
     * otherwise.
     */
    @WithDefault("1s")
    Duration hashSlotCacheTtl();

    /**
     * Whether automatic failover is enabled. This only makes sense for sentinel clients
     * with role of {@code MASTER} and is ignored otherwise.
     * <p>
     * If enabled, the sentinel client will additionally create a connection to one sentinel node
     * and watch for failover events. When new master is elected, all connections to the old master
     * are automatically closed and new connections to the new master are created. Automatic failover
     * makes sense for connections executing regular commands, but not for connections used to subscribe
     * to Redis pub/sub channels.
     * <p>
     * Note that there is a brief period of time between the old master failing and the new
     * master being elected when the existing connections will temporarily fail all operations.
     * After the new master is elected, the connections will automatically fail over and
     * start working again.
     */
    @WithDefault("false")
    boolean autoFailover();

    /**
     * How the Redis topology is obtained. By default, the topology is discovered automatically.
     * This is the only mode for the clustered and sentinel client. For replication client,
     * topology may be set <em>statically</em>.
     * <p>
     * In case of a static topology for replication Redis client, the first node in the list
     * is considered a <em>master</em> and the remaining nodes in the list are considered <em>replicas</em>.
     */
    @ConfigDocDefault("discover")
    Optional<RedisTopology> topology();

    /**
     * TCP config.
     */
    @ConfigDocSection
    NetConfig tcp();

    /**
     * SSL/TLS config.
     */
    @ConfigDocSection
    TlsConfig tls();

    /**
     * The client name used to identify the connection.
     * <p>
     * If the {@link RedisClientConfig#configureClientName()} is enabled, and this property is not set
     * it will attempt to extract the value from the {@link RedisClientName#value()} annotation.
     * <p>
     * If the {@link RedisClientConfig#configureClientName()} is enabled, both this property and the
     * {@link RedisClientName#value()} must adhere to the pattern '[a-zA-Z0-9\\-_.~]*'; if not,
     * this may result in an incorrect client name after URI encoding.
     */
    Optional<String> clientName();

    /**
     * Whether it should set the client name while connecting with Redis.
     * <p>
     * This is necessary because Redis only accepts {@code client=my-client-name} query parameter in version 6+.
     * <p>
     * This property can be used with {@link RedisClientConfig#clientName()} configuration.
     *
     */
    @WithDefault("false")
    Boolean configureClientName();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * If no TLS configuration name is set then, {@code quarkus.redis.$client-name.tls} will be used.
     * <p>
     * The default TLS configuration is <strong>not</strong> used by default.
     */
    Optional<String> tlsConfigurationName();

    default String toDebugString() {
        return "RedisClientConfig{" +
                "hosts=" + hosts() +
                ", hostsProviderName=" + hostsProviderName() +
                ", timeout=" + timeout() +
                ", clientType=" + clientType() +
                ", masterName=" + masterName() +
                ", role=" + role() +
                ", replicas=" + replicas() +
                ", password=" + password() +
                ", maxPoolSize=" + maxPoolSize() +
                ", maxPoolWaiting=" + maxPoolWaiting() +
                ", poolCleanerInterval=" + poolCleanerInterval() +
                ", poolRecycleTimeout=" + poolRecycleTimeout() +
                ", maxWaitingHandlers=" + maxWaitingHandlers() +
                ", maxNestedArrays=" + maxNestedArrays() +
                ", reconnectAttempts=" + reconnectAttempts() +
                ", reconnectInterval=" + reconnectInterval() +
                ", protocolNegotiation=" + protocolNegotiation() +
                ", preferredProtocolVersion=" + preferredProtocolVersion() +
                ", hashSlotCacheTtl=" + hashSlotCacheTtl() +
                ", tcp=" + tcp() +
                ", tls=" + tls() +
                ", clientName=" + clientName() +
                ", configureClientName=" + configureClientName() +
                '}';
    }

}

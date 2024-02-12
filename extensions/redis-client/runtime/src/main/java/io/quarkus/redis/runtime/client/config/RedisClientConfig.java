package io.quarkus.redis.runtime.client.config;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.vertx.redis.client.ProtocolVersion;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

@ConfigGroup
public interface RedisClientConfig {

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
     * The maximum delay to wait before a blocking command to redis server times out
     */
    @WithDefault("10s")
    Duration timeout();

    /**
     * The redis client type.
     * Accepted values are: {@code STANDALONE} (default), {@code CLUSTER}, {@code REPLICATION}, {@code SENTINEL}.
     */
    @WithDefault("standalone")
    RedisClientType clientType();

    /**
     * The master name (only considered in HA mode).
     */
    @ConfigDocDefault("mymaster")
    Optional<String> masterName();

    /**
     * The role name (only considered in Sentinel / HA mode).
     * Accepted values are: {@code MASTER}, {@code REPLICA}, {@code SENTINEL}.
     */
    @ConfigDocDefault("master")
    Optional<RedisRole> role();

    /**
     * Whether to use replicas nodes (only considered in Cluster mode).
     * Accepted values are: {@code ALWAYS}, {@code NEVER}, {@code SHARE}.
     */
    @ConfigDocDefault("never")
    Optional<RedisReplicas> replicas();

    /**
     * The default password for cluster/sentinel connections.
     * <p>
     * If not set it will try to extract the value from the current default {@code #hosts}.
     */
    Optional<String> password();

    /**
     * The maximum size of the connection pool. When working with cluster or sentinel.
     * <p>
     * This value should be at least the total number of cluster member (or number of sentinels + 1)
     */
    @WithDefault("6")
    int maxPoolSize();

    /**
     * The maximum waiting requests for a connection from the pool.
     */
    @WithDefault("24")
    int maxPoolWaiting();

    /**
     * The duration indicating how often should the connection pool cleaner executes.
     */
    Optional<Duration> poolCleanerInterval();

    /**
     * The timeout for a connection recycling.
     */
    @WithDefault("15")
    Duration poolRecycleTimeout();

    /**
     * Sets how many handlers is the client willing to queue.
     * <p>
     * The client will always work on pipeline mode, this means that messages can start queueing.
     * Using this configuration option, you can control how much backlog you're willing to accept.
     */
    @WithDefault("2048")
    int maxWaitingHandlers();

    /**
     * Tune how much nested arrays are allowed on a redis response. This affects the parser performance.
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
     * TCP config.
     */
    @ConfigDocSection
    NetConfig tcp();

    /**
     * SSL/TLS config.
     */
    @ConfigDocSection
    TlsConfig tls();

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
                '}';
    }

}

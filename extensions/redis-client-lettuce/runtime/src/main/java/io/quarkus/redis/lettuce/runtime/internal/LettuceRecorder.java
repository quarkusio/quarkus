package io.quarkus.redis.lettuce.runtime.internal;

import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.ActiveResult;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionFactory.MasterReplicaMode;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceBlockingRedisDataSourceImpl;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.quarkus.redis.runtime.client.config.NetConfig;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisRole;
import io.vertx.redis.client.RedisTopology;

/**
 * Quarkus recorder that manages the lifecycle of Lettuce Redis clients.
 * <p>
 * Creates {@link io.lettuce.core.resource.ClientResources} with shared Vert.x event loops, a
 * {@link LettuceConnectionFactory} per client configured from the {@code quarkus.redis[.<name>].*} properties the
 * Lettuce backend honours (see {@link LettuceClientSettings}): a {@link io.lettuce.core.RedisClient} for a
 * {@code standalone} client and for the master/replica connections of a {@code sentinel} or {@code replication} one,
 * a {@link io.lettuce.core.cluster.RedisClusterClient} for a {@code cluster} one. Also
 * creates, per client, the shared {@link LettuceConnection} of the data sources and a bounded
 * {@link LettuceConnectionPool} used for blocking commands and scoped connections
 * ({@code withConnection}/{@code withTransaction}) so they never occupy the shared connection.
 * <p>
 * Shutdown ordering: connections → pools → clients → resources (before Vert.x event loops).
 */
@Recorder
public class LettuceRecorder {

    private static final Logger LOGGER = Logger.getLogger(LettuceRecorder.class);
    private static final Duration POOL_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    /** The default of {@code master-name}, as documented for the Vert.x client. */
    private static final String DEFAULT_MASTER_NAME = "mymaster";

    private final RuntimeValue<RedisConfig> runtimeConfig;

    private static volatile LettuceClientResources sharedResources;
    private static volatile io.vertx.mutiny.core.Vertx mutinyVertx;
    private static final Map<String, LettuceConnectionFactory> factories = new ConcurrentHashMap<>();
    private static final Map<String, LettuceConnection> connections = new ConcurrentHashMap<>();
    private static final Map<String, StatefulConnection<String, String>> stringConnections = new ConcurrentHashMap<>();
    private static final Map<String, LettuceReactiveRedisDataSourceImpl> reactiveDataSources = new ConcurrentHashMap<>();
    private static final Map<String, LettuceConnectionPool> pools = new ConcurrentHashMap<>();

    public LettuceRecorder(RuntimeValue<RedisConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Initializes shared client resources and creates a Lettuce client for each requested client name.
     * Only creates clients that pass the {@link #checkActive(String)} check.
     */
    public void initialize(RuntimeValue<Vertx> vertx, Supplier<TlsConfigurationRegistry> tlsRegistry, Set<String> names) {
        EventLoopGroup eventLoopGroup = ((VertxInternal) vertx.getValue()).eventLoopGroup();
        sharedResources = new LettuceClientResources(eventLoopGroup);
        mutinyVertx = io.vertx.mutiny.core.Vertx.newInstance(vertx.getValue());

        for (String name : names) {
            if (checkActive(name).get().value()) {
                RedisClientConfig clientConfig = runtimeConfig.getValue().clients().get(name);
                // checkActive() guarantees at least one host for an active client
                List<URI> hosts = clientConfig.hosts().orElseThrow();
                warnAboutUnsupportedConfiguration(name, clientConfig, hosts);
                LettuceClientSettings settings = LettuceClientSettings.create(name, clientConfig, hosts, vertx.getValue(),
                        tlsRegistry.get());
                LettuceConnectionFactory factory = factories.computeIfAbsent(name,
                        k -> createFactory(name, clientConfig, settings));
                pools.putIfAbsent(name, new LettuceConnectionPool(factory::connectAsync,
                        clientConfig.maxPoolSize(), clientConfig.maxPoolWaiting(), factory.getDatabase()));
            }
        }
    }

    /**
     * The client of the configured {@code client-type}: a cluster client discovering the topology from all the
     * configured hosts; a Sentinel client following the master the configured hosts (the sentinels) monitor under
     * {@code master-name}; a replication client discovering the master and its replicas from the first host that
     * answers, the hosts being tried in order ({@code topology=discover}, the default, as the Vert.x client does) or
     * taking all the hosts as the nodes and asking each for its role ({@code topology=static}; unlike the Vert.x
     * client, the configured order is not trusted); a standalone client connecting to the first host.
     *
     * @throws ConfigurationException for {@code role=sentinel}: a sentinel has no data commands, so the data
     *         sources cannot use such a connection
     */
    private static LettuceConnectionFactory createFactory(String name, RedisClientConfig config,
            LettuceClientSettings settings) {
        ClientResources resources = sharedResources.clientResources();
        return switch (config.clientType()) {
            case CLUSTER -> new LettuceConnectionFactory(name, resources, settings.redisUris(), settings.clientOptions(),
                    LettuceClientSettings.topologyRefreshOptions(LettuceClientSettings.topologyCacheTtl(config)),
                    LettuceClientSettings.readFrom(config.replicas()), config.timeout());
            case SENTINEL -> {
                if (config.role().orElse(RedisRole.MASTER) == RedisRole.SENTINEL) {
                    throw new ConfigurationException(String.format(
                            "The Lettuce Redis client '%s' is configured with '%s=sentinel': a sentinel node has no data "
                                    + "commands, so the Redis data sources cannot use it. Set the property to 'master' or "
                                    + "'replica'.",
                            name, getPropertyName(name, "role")));
                }
                RedisURI master = settings.sentinelUri(config.masterName().orElse(DEFAULT_MASTER_NAME));
                yield new LettuceConnectionFactory(name, resources, MasterReplicaMode.SENTINEL, List.of(master),
                        settings.clientOptions(), LettuceClientSettings.readFrom(config.role(), config.replicas()),
                        config.timeout());
            }
            case REPLICATION -> {
                MasterReplicaMode mode = config.topology().orElse(RedisTopology.DISCOVER) == RedisTopology.STATIC
                        ? MasterReplicaMode.STATIC
                        : MasterReplicaMode.DISCOVER;
                yield new LettuceConnectionFactory(name, resources, mode, settings.redisUris(), settings.clientOptions(),
                        LettuceClientSettings.readFrom(config.replicas()), config.timeout());
            }
            case STANDALONE -> new LettuceConnectionFactory(name, resources, settings.redisUri(), settings.clientOptions(),
                    config.timeout());
        };
    }

    private static boolean isCluster(RedisClientConfig config) {
        return config.clientType() == RedisClientType.CLUSTER;
    }

    private static boolean isMasterReplica(RedisClientConfig config) {
        return config.clientType() == RedisClientType.SENTINEL || config.clientType() == RedisClientType.REPLICATION;
    }

    private static boolean hasHosts(RedisClientConfig config) {
        return config.hosts().isPresent() && !config.hosts().get().isEmpty();
    }

    /**
     * The Lettuce backend applies the hosts, client-type, timeout, active, username, password, TLS,
     * {@code tcp.secure-transport-protocols}, {@code max-pool-size} and {@code max-pool-waiting} properties; for a
     * cluster also {@code replicas} and {@code topology-cache-ttl}, but not the database of a host URI (a cluster
     * only has database 0); for a Sentinel client {@code master-name}, {@code role}, {@code auto-failover} (a
     * failover is always followed) and {@code replicas}, unless {@code role=replica} already decides where the reads
     * go; for a replication client {@code topology} and {@code replicas}. A standalone client only uses the first
     * host. Tell users at startup which other configured properties are not applied, instead of silently connecting
     * differently than configured. Properties with a default value are reported only when set to something else.
     */
    private static void warnAboutUnsupportedConfiguration(String name, RedisClientConfig config, List<URI> hosts) {
        List<String> ignored = new ArrayList<>();
        RedisClientType type = config.clientType();
        if (type == RedisClientType.CLUSTER) {
            // a cluster only has database 0; Lettuce ignores the database of the seed URIs rather than selecting it
            for (URI host : hosts) {
                if (RedisURI.create(host).getDatabase() != 0) {
                    ignored.add(getPropertyName(name, HOSTS) + " (the database of a URI: a cluster only has database 0)");
                    break;
                }
            }
        } else {
            // a Sentinel connection gets the topology from the sentinels, a replication connection keeps the one it
            // discovered: neither refreshes it periodically
            if (!LettuceClientSettings.topologyCacheTtl(config).equals(LettuceClientSettings.DEFAULT_TOPOLOGY_CACHE_TTL)) {
                ignored.add(getPropertyName(name, "topology-cache-ttl"));
            }
        }
        if (type == RedisClientType.STANDALONE) {
            if (hosts.size() > 1) {
                ignored.add(getPropertyName(name, HOSTS) + " (only the first URI is used)");
            }
            if (config.replicas().isPresent()) {
                ignored.add(getPropertyName(name, "replicas"));
            }
        }
        // role=replica sends the reads to the replicas whatever replicas says (see LettuceClientSettings.readFrom)
        if (type == RedisClientType.SENTINEL && config.role().orElse(RedisRole.MASTER) == RedisRole.REPLICA
                && config.replicas().isPresent()) {
            ignored.add(getPropertyName(name, "replicas") + " (role=replica reads from the replicas)");
        }
        if (type != RedisClientType.SENTINEL) {
            if (config.masterName().isPresent()) {
                ignored.add(getPropertyName(name, "master-name"));
            }
            if (config.role().isPresent()) {
                ignored.add(getPropertyName(name, "role"));
            }
            if (config.autoFailover()) {
                ignored.add(getPropertyName(name, "auto-failover"));
            }
        }
        if (type != RedisClientType.REPLICATION && config.topology().isPresent()) {
            ignored.add(getPropertyName(name, "topology"));
        }
        if (config.clusterTransactions().isPresent()) {
            ignored.add(getPropertyName(name, "cluster-transactions"));
        }
        if (config.poolCleanerInterval().isPresent()) {
            ignored.add(getPropertyName(name, "pool-cleaner-interval"));
        }
        if (config.poolRecycleTimeout().isPresent() && !config.poolRecycleTimeout().get().equals(Duration.ofMinutes(3))) {
            ignored.add(getPropertyName(name, "pool-recycle-timeout"));
        }
        if (config.maxWaitingHandlers() != 2048) {
            ignored.add(getPropertyName(name, "max-waiting-handlers"));
        }
        if (config.maxNestedArrays() != 32) {
            ignored.add(getPropertyName(name, "max-nested-arrays"));
        }
        if (config.reconnectAttempts() != 0) {
            ignored.add(getPropertyName(name, "reconnect-attempts"));
        }
        if (!config.reconnectInterval().equals(Duration.ofSeconds(1))) {
            ignored.add(getPropertyName(name, "reconnect-interval"));
        }
        if (!config.protocolNegotiation()) {
            ignored.add(getPropertyName(name, "protocol-negotiation"));
        }
        if (config.preferredProtocolVersion().isPresent()) {
            ignored.add(getPropertyName(name, "preferred-protocol-version"));
        }
        if (config.clientName().isPresent()) {
            ignored.add(getPropertyName(name, "client-name"));
        }
        if (config.configureClientName()) {
            ignored.add(getPropertyName(name, "configure-client-name"));
        }
        for (String tcpProperty : configuredTcpProperties(config.tcp())) {
            ignored.add(getPropertyName(name, "tcp." + tcpProperty));
        }
        if (!ignored.isEmpty()) {
            LOGGER.warnf("Lettuce Redis client '%s': the following configuration is not applied by the Lettuce backend yet: %s",
                    name, String.join(", ", ignored));
        }
    }

    /**
     * The names of the {@code tcp.*} properties that are set, except {@code secure-transport-protocols} which is
     * applied to the TLS handshake.
     */
    private static List<String> configuredTcpProperties(NetConfig tcp) {
        Map<String, Boolean> present = new LinkedHashMap<>();
        present.put("alpn", tcp.alpn().isPresent());
        present.put("application-layer-protocols", tcp.applicationLayerProtocols().isPresent());
        present.put("idle-timeout", tcp.idleTimeout().isPresent());
        present.put("connection-timeout", tcp.connectionTimeout().isPresent());
        present.put("proxy-configuration-name", tcp.proxyConfigurationName().isPresent());
        present.put("read-idle-timeout", tcp.readIdleTimeout().isPresent());
        present.put("receive-buffer-size", tcp.receiveBufferSize().isPresent());
        // The defaults of the client-level reconnect-attempts (0) and reconnect-interval (1s) are registered for every
        // client as quarkus.redis.*.reconnect-attempts and quarkus.redis.*.reconnect-interval, which the tcp group of
        // the default client (quarkus.redis.tcp.*) also matches: both tcp properties are always present for that
        // client. Treat them like defaulted properties and report them only when set to something else.
        present.put("reconnect-attempts", tcp.reconnectAttempts().isPresent() && tcp.reconnectAttempts().getAsInt() != 0);
        present.put("reconnect-interval",
                tcp.reconnectInterval().isPresent() && !tcp.reconnectInterval().get().equals(Duration.ofSeconds(1)));
        present.put("reuse-address", tcp.reuseAddress().isPresent());
        present.put("reuse-port", tcp.reusePort().isPresent());
        present.put("send-buffer-size", tcp.sendBufferSize().isPresent());
        present.put("so-linger", tcp.soLinger().isPresent());
        present.put("cork", tcp.cork().isPresent());
        present.put("fast-open", tcp.fastOpen().isPresent());
        present.put("keep-alive", tcp.keepAlive().isPresent());
        present.put("no-delay", tcp.noDelay().isPresent());
        present.put("quick-ack", tcp.quickAck().isPresent());
        present.put("traffic-class", tcp.trafficClass().isPresent());
        present.put("write-idle-timeout", tcp.writeIdleTimeout().isPresent());
        present.put("local-address", tcp.localAddress().isPresent());
        List<String> configured = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : present.entrySet()) {
            if (entry.getValue()) {
                configured.add(entry.getKey());
            }
        }
        return configured;
    }

    public Supplier<Object> getClientResources() {
        return () -> sharedResources.clientResources();
    }

    public Supplier<Object> getRedisClient(String name) {
        return () -> factories.get(name).getRedisClient();
    }

    public Supplier<Object> getClusterClient(String name) {
        return () -> factories.get(name).getClusterClient();
    }

    /**
     * The {@code StatefulRedisConnection<String, String>} bean of a standalone, Sentinel or replication client (for
     * the last two also exposed as {@code StatefulRedisMasterReplicaConnection<String, String>}), or the
     * {@code StatefulRedisClusterConnection<String, String>} bean of a cluster client: the beans of the other
     * topologies are inactive (see {@link #checkActiveStandalone}, {@link #checkActiveMasterReplica} and
     * {@link #checkActiveCluster}), so a client has one such connection.
     */
    public Supplier<Object> getConnection(String name) {
        return () -> stringConnections.computeIfAbsent(name, k -> {
            StatefulConnection<String, String> connection = factories.get(k).connect(StringCodec.UTF8);
            LOGGER.infof("Opened %s for client '%s'", connection.getClass().getSimpleName(), k);
            return connection;
        });
    }

    private static LettuceConnection dataSourceConnection(String name) {
        return connections.computeIfAbsent(name, k -> {
            LOGGER.infof("Opening data source connection for client '%s'", k);
            return factories.get(k).connect();
        });
    }

    public Supplier<ReactiveRedisDataSource> getReactiveDataSource(String name) {
        return () -> reactiveDataSources.computeIfAbsent(name, k -> {
            LettuceConnection conn = dataSourceConnection(k);
            LettuceConnectionFactory factory = factories.get(k);
            return new LettuceReactiveRedisDataSourceImpl(mutinyVertx, conn, pools.get(k), factory::connectPubSubAsync);
        });
    }

    public Supplier<RedisDataSource> getBlockingDataSource(String name) {
        return () -> {
            Duration timeout = runtimeConfig.getValue().clients().get(name).timeout();
            return new LettuceBlockingRedisDataSourceImpl(
                    (LettuceReactiveRedisDataSourceImpl) getReactiveDataSource(name).get(), timeout);
        };
    }

    public Supplier<ActiveResult> checkActive(final String name) {
        return () -> {
            RedisClientConfig redisClientConfig = runtimeConfig.getValue().clients().get(name);
            if (!redisClientConfig.active()) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' was deactivated through configuration properties. \
                                To activate the Redis Client, set configuration property '%s' to 'true' and configure the Redis Client '%s'. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "active"), name));
            }
            if (!hasHosts(redisClientConfig)) {
                if (redisClientConfig.hostsProviderName().isPresent()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Lettuce Redis Client '%s' was deactivated automatically because it is configured through '%s', \
                                    which the Lettuce backend does not support yet. Set the configuration property '%s' instead. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """,
                            name, getPropertyName(name, HOSTS_PROVIDER_NAME), getPropertyName(name, HOSTS)));
                }
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' was deactivated automatically because the hosts are not set. \
                                To activate the Redis Client, set the configuration property '%s'. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, HOSTS)));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code RedisClient} and {@code StatefulRedisConnection} beans: those of an active
     * client that is not configured as a cluster (a master/replica connection is a {@code StatefulRedisConnection}).
     */
    public Supplier<ActiveResult> checkActiveStandalone(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            if (isCluster(runtimeConfig.getValue().clients().get(name))) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is configured as a cluster through the configuration property '%s'. \
                                Inject io.lettuce.core.cluster.RedisClusterClient and \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String> instead of \
                                io.lettuce.core.RedisClient and io.lettuce.core.api.StatefulRedisConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type")));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code StatefulRedisMasterReplicaConnection} bean: that of an active client that is
     * configured as a Sentinel or replication client.
     */
    public Supplier<ActiveResult> checkActiveMasterReplica(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            RedisClientConfig config = runtimeConfig.getValue().clients().get(name);
            if (!isMasterReplica(config)) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is not configured as a Sentinel or replication client (its '%s' is '%s'): \
                                inject io.lettuce.core.api.StatefulRedisConnection<String, String> for a standalone client or \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String> for a cluster instead of \
                                io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type"), config.clientType().name().toLowerCase()));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code RedisClusterClient} and {@code StatefulRedisClusterConnection} beans: those
     * of an active client that is configured as a cluster.
     */
    public Supplier<ActiveResult> checkActiveCluster(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            if (!isCluster(runtimeConfig.getValue().clients().get(name))) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is not configured as a cluster: set the configuration property '%s' to 'cluster', \
                                or inject io.lettuce.core.RedisClient and io.lettuce.core.api.StatefulRedisConnection<String, String> \
                                instead of io.lettuce.core.cluster.RedisClusterClient and \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type")));
            }
            return ActiveResult.active();
        };
    }

    public void cleanup(ShutdownContext context) {
        context.addShutdownTask(() -> {
            closeConnections(connections);
            closeConnections(stringConnections);
            reactiveDataSources.clear();

            for (Map.Entry<String, LettuceConnectionPool> entry : pools.entrySet()) {
                try {
                    entry.getValue().close().await().atMost(POOL_CLOSE_TIMEOUT);
                } catch (Exception e) {
                    LOGGER.warnf(e, "Error closing Lettuce connection pool for '%s'", entry.getKey());
                }
            }
            pools.clear();

            for (Map.Entry<String, LettuceConnectionFactory> entry : factories.entrySet()) {
                try {
                    entry.getValue().shutdown();
                } catch (Exception e) {
                    LOGGER.warnf(e, "Error shutting down Lettuce client for '%s'", entry.getKey());
                }
            }
            factories.clear();

            if (sharedResources != null) {
                sharedResources.shutdown();
                sharedResources = null;
            }
            mutinyVertx = null;
        });
    }

    private static void closeConnections(Map<String, ? extends AutoCloseable> connectionsByClient) {
        for (Map.Entry<String, ? extends AutoCloseable> entry : connectionsByClient.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LOGGER.warnf(e, "Error closing Lettuce connection for client '%s'", entry.getKey());
            }
        }
        connectionsByClient.clear();
    }
}

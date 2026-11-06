package io.quarkus.redis.lettuce.runtime.internal;

import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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
import io.quarkus.redis.runtime.client.VertxRedisClientFactory;
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
 * ({@code withConnection}/{@code withTransaction}) so they never occupy the shared connection, its idle connections
 * recycled as configured by {@code pool-recycle-timeout} and {@code pool-cleaner-interval}.
 * <p>
 * Shutdown ordering: connections → pools → clients → resources (before Vert.x event loops).
 */
@Recorder
public class LettuceRecorder {

    private static final Logger LOGGER = Logger.getLogger(LettuceRecorder.class);
    private static final Duration POOL_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    /** The default of {@code master-name}, as documented for the Vert.x client. */
    private static final String DEFAULT_MASTER_NAME = "mymaster";
    /** The default of {@code pool-recycle-timeout}. */
    private static final Duration DEFAULT_POOL_RECYCLE_TIMEOUT = Duration.ofMinutes(3);
    /** The default of the Vert.x client's pool cleaner interval, applied when {@code pool-cleaner-interval} is unset. */
    private static final Duration DEFAULT_POOL_CLEANER_INTERVAL = Duration.ofSeconds(30);
    /** Why {@code reconnect-attempts} and {@code tcp.reconnect-attempts} are not applied. */
    private static final String RECONNECT_ATTEMPTS_REASON = " (Lettuce reconnects a lost connection until it succeeds"
            + " and does not retry the initial connection)";

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
     * Only creates clients that pass the {@link #checkActive(String)} check. Called once the synthetic beans are
     * initialized (see {@code LettuceProcessor}): the hosts may come from a {@code RedisHostsProvider} bean (see
     * {@link #hosts}), which may itself inject beans that are only usable from then on.
     */
    public void initialize(RuntimeValue<Vertx> vertx, Supplier<TlsConfigurationRegistry> tlsRegistry, Set<String> names) {
        EventLoopGroup eventLoopGroup = ((VertxInternal) vertx.getValue()).eventLoopGroup();
        sharedResources = new LettuceClientResources(eventLoopGroup);
        mutinyVertx = io.vertx.mutiny.core.Vertx.newInstance(vertx.getValue());

        for (String name : names) {
            if (checkActive(name).get().value()) {
                RedisClientConfig clientConfig = runtimeConfig.getValue().clients().get(name);
                List<URI> hosts = hosts(name, clientConfig);
                warnAboutUnsupportedConfiguration(name, clientConfig, hosts);
                LettuceClientSettings settings = LettuceClientSettings.create(name, clientConfig, hosts, vertx.getValue(),
                        tlsRegistry.get());
                LettuceConnectionFactory factory = factories.computeIfAbsent(name,
                        k -> createFactory(name, clientConfig, settings));
                // computeIfAbsent, like the factories: a pool schedules its cleaner when it is built, and a pool built
                // only to be discarded by putIfAbsent would leave that timer running for good
                pools.computeIfAbsent(name, k -> new LettuceConnectionPool(factory::connectAsync,
                        clientConfig.maxPoolSize(), clientConfig.maxPoolWaiting(), factory.getDatabase(), vertx.getValue(),
                        clientConfig.poolRecycleTimeout().orElse(DEFAULT_POOL_RECYCLE_TIMEOUT),
                        clientConfig.poolCleanerInterval().orElse(DEFAULT_POOL_CLEANER_INTERVAL)));
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
        ClientResources resources = sharedResources.clientResources(name, settings.reconnectDelay(),
                settings.nettyCustomizer());
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
     * The hosts of an active client: {@code hosts} when set, else those of the {@code RedisHostsProvider} bean
     * {@code hosts-provider-name} names, resolved as the Vert.x client resolves it (the same lookup and the same
     * failure when the bean is missing). {@code hosts} wins when both are set, as with the Vert.x client.
     * {@link #checkActive(String)} guarantees one of the two is set.
     *
     * @throws ConfigurationException if the provider bean is missing or returns no host
     */
    private static List<URI> hosts(String name, RedisClientConfig config) {
        if (hasHosts(config)) {
            return config.hosts().get();
        }
        String providerName = config.hostsProviderName().orElseThrow();
        Collection<URI> hosts = VertxRedisClientFactory.findProvider(providerName).getHosts();
        if (hosts == null || hosts.isEmpty()) {
            throw new ConfigurationException(String.format(
                    "The Redis hosts provider '%s' of the Lettuce Redis client '%s' (configuration property '%s') "
                            + "returned no host",
                    providerName, name, getPropertyName(name, HOSTS_PROVIDER_NAME)));
        }
        return new ArrayList<>(hosts);
    }

    /**
     * The Lettuce backend applies the hosts (or {@code hosts-provider-name}), client-type, timeout, active, username,
     * password,
     * TLS, client name, protocol, command queue and TCP socket properties (see {@link LettuceClientSettings}),
     * {@code max-pool-size}, {@code max-pool-waiting}, {@code pool-recycle-timeout} and {@code pool-cleaner-interval}
     * (see {@link LettuceConnectionPool}); for a cluster also {@code replicas} and {@code topology-cache-ttl}, but not the
     * database of a host URI (a cluster only has database 0); for a Sentinel client {@code master-name},
     * {@code role}, {@code auto-failover} (a failover is always followed) and {@code replicas}, unless
     * {@code role=replica} already decides where the reads go; for a replication client {@code topology} and
     * {@code replicas}. A standalone client only uses the first host. Tell users at startup which other configured
     * properties are not applied, and why, instead of silently connecting differently than configured. Properties
     * with a default value are reported only when set to something else.
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
            ignored.add(getPropertyName(name, "cluster-transactions")
                    + " (Lettuce does not route MULTI/EXEC on a cluster, so transactions are not supported there)");
        }
        if (config.maxNestedArrays() != 32) {
            ignored.add(getPropertyName(name, "max-nested-arrays") + " (Lettuce does not limit the nesting of replies)");
        }
        if (config.reconnectAttempts() != 0) {
            ignored.add(getPropertyName(name, "reconnect-attempts") + RECONNECT_ATTEMPTS_REASON);
        }
        // applied only with configure-client-name, as with the Vert.x client (see LettuceClientSettings.clientName)
        if (config.clientName().isPresent() && !config.configureClientName()) {
            ignored.add(getPropertyName(name, "client-name") + " (applied only with "
                    + getPropertyName(name, "configure-client-name") + "=true, as with the Vert.x client)");
        }
        for (String tcpProperty : unsupportedTcpProperties(config.tcp())) {
            ignored.add(getPropertyName(name, "tcp." + tcpProperty));
        }
        if (!ignored.isEmpty()) {
            LOGGER.warnf("Lettuce Redis client '%s': the following configuration is not applied by the Lettuce backend: %s",
                    name, String.join(", ", ignored));
        }
    }

    /**
     * The {@code tcp.*} properties that are set and have no Lettuce equivalent, each with the reason, except
     * {@code secure-transport-protocols} (applied to the TLS handshake), {@code connection-timeout} (when positive),
     * {@code keep-alive}, {@code no-delay}, {@code reconnect-interval}, {@code receive-buffer-size},
     * {@code send-buffer-size}, {@code so-linger}, {@code traffic-class}, {@code reuse-address} and
     * {@code local-address}, which are applied (see {@link LettuceClientSettings}).
     */
    private static List<String> unsupportedTcpProperties(NetConfig tcp) {
        Map<String, Boolean> present = new LinkedHashMap<>();
        // the Vert.x client takes a connect timeout of zero as no timeout; Lettuce requires a positive one (see
        // LettuceClientSettings.socketOptions)
        present.put("connection-timeout (Lettuce requires a positive connect timeout, so its default of ten seconds"
                + " applies)",
                tcp.connectionTimeout().isPresent() && !LettuceClientSettings.isPositive(tcp.connectionTimeout().get()));
        String noAlpn = " (the Redis protocol has no ALPN)";
        present.put("alpn" + noAlpn, tcp.alpn().isPresent());
        present.put("application-layer-protocols" + noAlpn, tcp.applicationLayerProtocols().isPresent());
        String noIdleHandler = " (Lettuce does not close idle connections)";
        present.put("idle-timeout" + noIdleHandler, tcp.idleTimeout().isPresent());
        present.put("read-idle-timeout" + noIdleHandler, tcp.readIdleTimeout().isPresent());
        present.put("write-idle-timeout" + noIdleHandler, tcp.writeIdleTimeout().isPresent());
        String noProxy = " (Lettuce does not connect through a proxy)";
        present.put("proxy-configuration-name" + noProxy, tcp.proxyConfigurationName().isPresent());
        // The default of the client-level reconnect-attempts (0) is registered for every client as
        // quarkus.redis.*.reconnect-attempts, which the tcp group of the default client (quarkus.redis.tcp.*) also
        // matches: the tcp property is always present for that client. Treat it like a defaulted property and report
        // it only when set to something else.
        present.put("reconnect-attempts" + RECONNECT_ATTEMPTS_REASON,
                tcp.reconnectAttempts().isPresent() && tcp.reconnectAttempts().getAsInt() != 0);
        String nativeTransport = " (an option of the Linux native transport)";
        present.put("reuse-port" + nativeTransport, tcp.reusePort().isPresent());
        present.put("cork" + nativeTransport, tcp.cork().isPresent());
        present.put("fast-open" + nativeTransport, tcp.fastOpen().isPresent());
        present.put("quick-ack" + nativeTransport, tcp.quickAck().isPresent());
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
            if (!hasHosts(redisClientConfig) && redisClientConfig.hostsProviderName().isEmpty()) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' was deactivated automatically because the hosts are not set. \
                                To activate the Redis Client, set the configuration property '%s', or '%s' together with \
                                a RedisHostsProvider bean of that name. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, HOSTS), getPropertyName(name, HOSTS_PROVIDER_NAME)));
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

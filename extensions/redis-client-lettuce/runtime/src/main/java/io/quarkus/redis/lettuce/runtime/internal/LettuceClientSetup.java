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
import java.util.Optional;

import org.jboss.logging.Logger;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionFactory.MasterReplicaMode;
import io.quarkus.redis.runtime.client.VertxRedisClientFactory;
import io.quarkus.redis.runtime.client.config.NetConfig;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisRole;
import io.vertx.redis.client.RedisTopology;

/**
 * Creates the pieces of a Lettuce Redis client from the {@code quarkus.redis[.<name>].*} properties the Lettuce backend
 * honours (see {@link LettuceClientSettings}): the shared {@link LettuceClientResources} built on the Vert.x event
 * loops, a {@link LettuceConnectionFactory} per client (a {@link io.lettuce.core.RedisClient} for a {@code standalone}
 * client and for the master/replica connections of a {@code sentinel} or {@code replication} one, a
 * {@link io.lettuce.core.cluster.RedisClusterClient} for a {@code cluster} one), the {@link LettuceSharedConnection}
 * of the data sources, which their first command opens, and the bounded {@link LettuceConnectionPool} used for blocking
 * commands and scoped connections
 * ({@code withConnection}/{@code withTransaction}) so they never occupy the shared connection, its idle connections
 * recycled as configured by {@code pool-recycle-timeout} and {@code pool-cleaner-interval}.
 * <p>
 * Called from the services {@code LettuceProcessor} registers, one per piece, which own the lifecycle: a service stops
 * before the services it depends on, so the connections and pools close before the clients shut down, and the clients
 * before the shared resources (and those before the Vert.x event loops).
 */
public final class LettuceClientSetup {

    private static final Logger LOGGER = Logger.getLogger(LettuceClientSetup.class);
    private static final Duration POOL_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    /** The default of {@code master-name}, as documented for the Vert.x client. */
    private static final String DEFAULT_MASTER_NAME = "mymaster";
    /** The default of {@code pool-recycle-timeout}, applied when the property is blank. */
    private static final Duration DEFAULT_POOL_RECYCLE_TIMEOUT = Duration.ofMinutes(3);
    /** The default of the Vert.x client's pool cleaner interval, applied when {@code pool-cleaner-interval} is unset. */
    private static final Duration DEFAULT_POOL_CLEANER_INTERVAL = Duration.ofSeconds(30);
    /** Why {@code reconnect-attempts} and {@code tcp.reconnect-attempts} are not applied. */
    private static final String RECONNECT_ATTEMPTS_REASON = " (Lettuce reconnects a lost connection until it succeeds;"
            + " a connection that cannot be opened fails the commands waiting for it, and the next command tries again)";

    private LettuceClientSetup() {
    }

    /**
     * Whether a client is active: {@code active} is set and either {@code hosts} or {@code hosts-provider-name} is
     * configured (see {@link LettuceRecorder#checkActive}, which tells the user why a bean is inactive).
     */
    public static boolean isActive(RedisClientConfig config) {
        return config.active() && (hasHosts(config) || config.hostsProviderName().isPresent());
    }

    public static boolean hasHosts(RedisClientConfig config) {
        return config.hosts().isPresent() && !config.hosts().get().isEmpty();
    }

    /**
     * The shared client resources, on the event loops of the given Vert.x instance, once Lettuce's Netty transport is
     * aligned with the one Vert.x runs on (see {@link LettuceTransport}).
     */
    public static LettuceClientResources createResources(Vertx vertx) {
        LettuceTransport.align(vertx);
        return new LettuceClientResources(((VertxInternal) vertx).eventLoopGroup());
    }

    /**
     * The client of the configured {@code client-type}: a cluster client discovering the topology from all the
     * configured hosts; a Sentinel client following the master the configured hosts (the sentinels) monitor under
     * {@code master-name}; a replication client discovering the master and its replicas from the first host that
     * answers, the hosts being tried in order ({@code topology=discover}, the default, as the Vert.x client does) or
     * taking all the hosts as the nodes and asking each for its role ({@code topology=static}; unlike the Vert.x
     * client, the configured order is not trusted); a standalone client connecting to the first host. The hosts may
     * come from a {@code RedisHostsProvider} bean (see {@link #hosts}), so the CDI container must be running.
     *
     * @throws ConfigurationException for {@code role=sentinel}: a sentinel has no data commands, so the data
     *         sources cannot use such a connection
     */
    public static LettuceConnectionFactory createFactory(String name, RedisClientConfig config,
            LettuceClientResources sharedResources, Vertx vertx, TlsConfigurationRegistry tlsRegistry) {
        List<URI> hosts = hosts(name, config);
        warnAboutUnsupportedConfiguration(name, config, hosts);
        LettuceClientSettings settings = LettuceClientSettings.create(name, config, hosts, vertx, tlsRegistry);
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

    /**
     * The {@link RedisClient} of a standalone, Sentinel or replication client, empty for a cluster client.
     */
    public static Optional<RedisClient> redisClient(LettuceConnectionFactory factory) {
        return factory.getClient() instanceof RedisClient client ? Optional.of(client) : Optional.empty();
    }

    /**
     * The {@link RedisClusterClient} of a cluster client, empty for the other client types.
     */
    public static Optional<RedisClusterClient> clusterClient(LettuceConnectionFactory factory) {
        return factory.getClient() instanceof RedisClusterClient client ? Optional.of(client) : Optional.empty();
    }

    /**
     * The {@code StatefulRedisConnection<String, String>} of a standalone, Sentinel or replication client (for the last
     * two also exposed as {@code StatefulRedisMasterReplicaConnection<String, String>}), or the
     * {@code StatefulRedisClusterConnection<String, String>} of a cluster client: the beans of the other topologies
     * are inactive (see {@link LettuceRecorder#checkActiveStandalone}, {@link LettuceRecorder#checkActiveMasterReplica}
     * and {@link LettuceRecorder#checkActiveCluster}), so a client has one such connection.
     */
    public static StatefulConnection<String, String> connectStrings(String name, LettuceConnectionFactory factory) {
        StatefulConnection<String, String> connection = factory.connect(StringCodec.UTF8);
        LOGGER.infof("Opened %s for client '%s'", connection.getClass().getSimpleName(), name);
        return connection;
    }

    /**
     * The shared connection of the data sources of a client. Nothing connects here: the first command opens it, as
     * with the Vert.x data sources, and a command finding the server away fails while the next one tries again.
     */
    public static LettuceSharedConnection sharedConnection(String name, LettuceConnectionFactory factory) {
        return new LettuceSharedConnection(() -> {
            LOGGER.infof("Opening data source connection for client '%s'", name);
            return factory.connectAsync();
        }, factory.isCluster());
    }

    /**
     * The connection pool of the data sources of a client, sized and recycled as configured.
     */
    public static LettuceConnectionPool createPool(RedisClientConfig config, LettuceConnectionFactory factory,
            Vertx vertx) {
        return new LettuceConnectionPool(factory::connectAsync, config.maxPoolSize(), config.maxPoolWaiting(),
                factory.getDatabase(), vertx,
                config.poolRecycleTimeout().orElse(DEFAULT_POOL_RECYCLE_TIMEOUT),
                config.poolCleanerInterval().orElse(DEFAULT_POOL_CLEANER_INTERVAL));
    }

    /**
     * Closes a pool, waiting for its connections to close.
     */
    public static void closePool(LettuceConnectionPool pool) {
        pool.close().await().atMost(POOL_CLOSE_TIMEOUT);
    }

    /**
     * The hosts of an active client: {@code hosts} when set, else those of the {@code RedisHostsProvider} bean
     * {@code hosts-provider-name} names, resolved as the Vert.x client resolves it (the same lookup and the same
     * failure when the bean is missing). {@code hosts} wins when both are set, as with the Vert.x client.
     * {@link #isActive} guarantees one of the two is set. A host using a scheme the Lettuce backend does not support
     * is rejected here (see {@link LettuceClientSettings#rejectUnsupportedScheme}).
     *
     * @throws ConfigurationException if the provider bean is missing or returns no host, or a host uses an unsupported
     *         scheme
     */
    private static List<URI> hosts(String name, RedisClientConfig config) {
        List<URI> hosts;
        if (hasHosts(config)) {
            hosts = config.hosts().get();
        } else {
            String providerName = config.hostsProviderName().orElseThrow();
            Collection<URI> provided = VertxRedisClientFactory.findProvider(providerName).getHosts();
            if (provided == null || provided.isEmpty()) {
                throw new ConfigurationException(String.format(
                        "The Redis hosts provider '%s' of the Lettuce Redis client '%s' (configuration property '%s') "
                                + "returned no host",
                        providerName, name, getPropertyName(name, HOSTS_PROVIDER_NAME)));
            }
            hosts = new ArrayList<>(provided);
        }
        for (URI host : hosts) {
            LettuceClientSettings.rejectUnsupportedScheme(name, host);
        }
        return hosts;
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
     * properties are not applied, and why, instead of silently connecting differently than configured. A property is
     * reported only when the configuration sets it (see {@link LettuceClientSettings#isSet}): one left to its upstream
     * default is not, and that default is never copied here, so a change of it upstream cannot start or stop a warning.
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
            if (LettuceClientSettings.isSet(getPropertyName(name, "topology-cache-ttl"))) {
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
        if (LettuceClientSettings.isSet(getPropertyName(name, "max-nested-arrays"))) {
            ignored.add(getPropertyName(name, "max-nested-arrays") + " (Lettuce does not limit the nesting of replies)");
        }
        if (LettuceClientSettings.isSet(getPropertyName(name, "reconnect-attempts"))) {
            ignored.add(getPropertyName(name, "reconnect-attempts") + RECONNECT_ATTEMPTS_REASON);
        }
        // applied only with configure-client-name, as with the Vert.x client (see LettuceClientSettings.clientName)
        if (config.clientName().isPresent() && !config.configureClientName()) {
            ignored.add(getPropertyName(name, "client-name") + " (applied only with "
                    + getPropertyName(name, "configure-client-name") + "=true, as with the Vert.x client)");
        }
        for (String tcpProperty : unsupportedTcpProperties(name, config.tcp())) {
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
    private static List<String> unsupportedTcpProperties(String name, NetConfig tcp) {
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
        // The default of the client-level reconnect-attempts is registered for every client as
        // quarkus.redis.*.reconnect-attempts, which the tcp group of the default client (quarkus.redis.tcp.*) also
        // matches: the mapped tcp property is always present for that client, with the default. Ask the configuration
        // whether it is set instead (see LettuceClientSettings.isSet).
        present.put("reconnect-attempts" + RECONNECT_ATTEMPTS_REASON,
                LettuceClientSettings.isSet(getPropertyName(name, "tcp.reconnect-attempts")));
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
}

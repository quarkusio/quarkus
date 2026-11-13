package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TransactionalCommand;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.vertx.core.Context;

/**
 * Factory for the connections of one Quarkus Redis client, backed by a Lettuce {@link RedisClient} (standalone,
 * Sentinel and replication) or {@link RedisClusterClient} (cluster) created with shared {@link ClientResources}.
 * <p>
 * The connections it opens are {@link LettuceConnection}s, the same handle whatever the topology, so the data
 * sources and the {@link LettuceConnectionPool} do not depend on the client type. The client type decides the
 * {@link Topology}: a cluster client discovers the topology from the configured seed nodes, refreshes it
 * periodically and on {@code MOVED}/{@code ASK} redirects and reconnects (see
 * {@link ClusterTopologyRefreshOptions}), and reads from the nodes selected by its {@link ReadFrom}; a Sentinel or
 * replication client opens master/replica connections (see {@link io.lettuce.core.masterreplica.MasterReplica})
 * that write to the master and read from the nodes selected by their {@link ReadFrom}.
 * <p>
 * The client is created with externally managed {@link ClientResources} (which use Vert.x event loops).
 * The caller is responsible for shutting down the client before shutting down the {@link ClientResources}.
 */
public class LettuceConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(LettuceConnectionFactory.class);

    /**
     * How the master and the replicas of a Sentinel or replication client are found, see
     * {@link #LettuceConnectionFactory(String, ClientResources, MasterReplicaMode, List, ClientOptions, ReadFrom, Duration)}.
     */
    public enum MasterReplicaMode {
        /** The sentinels of a single sentinel URI report the master and its replicas, and every failover. */
        SENTINEL,
        /**
         * The master and its replicas are discovered ({@code INFO replication}) from the first Redis URI that answers,
         * the URIs being tried in order.
         */
        DISCOVER,
        /** The Redis URIs are the nodes, each asked for its role. */
        STATIC
    }

    private final String clientName;
    private final Topology topology;

    /**
     * Creates a Lettuce {@link RedisClient} using the given shared resources, Redis URI, client options and command
     * timeout.
     *
     * @param clientName the Quarkus Redis client name, used for logging
     * @param clientResources shared client resources (with Vert.x event loops)
     * @param redisUri the Redis URI, carrying the host, the credentials and the TLS mode
     * @param clientOptions the client options, carrying the TLS material (see {@link LettuceClientSettings}); the
     *        command timeout options are added to them
     * @param timeout the {@code quarkus.redis.timeout} applied to non-blocking commands
     */
    public LettuceConnectionFactory(String clientName, ClientResources clientResources, RedisURI redisUri,
            ClientOptions clientOptions, Duration timeout) {
        LOGGER.infof("Creating Lettuce RedisClient '%s' for %s:%d%s", clientName, redisUri.getHost(), redisUri.getPort(),
                redisUri.isSsl() ? " (TLS)" : "");
        RedisClient client = RedisClient.create(clientResources, redisUri);
        client.setOptions(withCommandTimeout(clientOptions, timeout));
        this.clientName = clientName;
        this.topology = new Standalone(client, redisUri);
    }

    /**
     * Creates a Lettuce {@link RedisClusterClient} using the given shared resources, seed nodes, client options,
     * topology refresh options, read preference and command timeout.
     * <p>
     * Lettuce connects to the nodes it discovers with the settings of the first seed URI (credentials, TLS), so all
     * the seeds are expected to carry the same ones, as {@link LettuceClientSettings} produces them. A cluster only
     * has database {@code 0}: the database of the seed URIs is ignored.
     *
     * @param clientName the Quarkus Redis client name, used for logging
     * @param clientResources shared client resources (with Vert.x event loops)
     * @param seeds the URIs of the cluster nodes to discover the topology from; need not be all the nodes
     * @param clientOptions the client options, carrying the TLS material (see {@link LettuceClientSettings}); the
     *        command timeout options and the topology refresh options are added to them
     * @param topologyRefresh when the cluster topology is refreshed (see
     *        {@link LettuceClientSettings#topologyRefreshOptions})
     * @param readFrom the nodes read-only commands are sent to (see {@link LettuceClientSettings#readFrom})
     * @param timeout the {@code quarkus.redis.timeout} applied to non-blocking commands
     */
    public LettuceConnectionFactory(String clientName, ClientResources clientResources, List<RedisURI> seeds,
            ClientOptions clientOptions, ClusterTopologyRefreshOptions topologyRefresh, ReadFrom readFrom,
            Duration timeout) {
        nonNull(seeds, "seeds");
        nonNull(topologyRefresh, "topologyRefresh");
        nonNull(readFrom, "readFrom");
        if (seeds.isEmpty()) {
            throw new IllegalArgumentException("At least one seed node is required for the Redis cluster client "
                    + clientName);
        }
        LOGGER.infof("Creating Lettuce RedisClusterClient '%s' for the seed nodes %s%s, reading from %s", clientName,
                describe(seeds), seeds.getFirst().isSsl() ? " (TLS)" : "", describe(readFrom));
        RedisClusterClient client = RedisClusterClient.create(clientResources, seeds);
        client.setOptions(ClusterClientOptions.builder(withCommandTimeout(clientOptions, timeout))
                .topologyRefreshOptions(topologyRefresh)
                .build());
        this.clientName = clientName;
        this.topology = new Cluster(client, readFrom);
    }

    /**
     * Creates a Lettuce {@link RedisClient} whose connections are master/replica connections built by
     * {@link io.lettuce.core.masterreplica.MasterReplica}, the nodes found as {@code mode} says:
     * <ul>
     * <li>{@link MasterReplicaMode#SENTINEL}: {@code nodes} is a single URI with sentinels (see
     * {@link LettuceClientSettings#sentinelUri}); the connections follow the master the sentinels monitor, and the
     * replicas they report; Lettuce subscribes to the sentinel events, so a failover is followed without any
     * reconnection by the caller;</li>
     * <li>{@link MasterReplicaMode#DISCOVER}: the master and its replicas are discovered ({@code INFO replication})
     * when a connection is opened, from the first of the Redis URIs in {@code nodes} that answers, the URIs being
     * tried in order as the Vert.x replication client tries its endpoints;</li>
     * <li>{@link MasterReplicaMode#STATIC}: the Redis URIs in {@code nodes} are the nodes, however many, each asked
     * for its role when a connection is opened.</li>
     * </ul>
     * The last two keep the topology they discovered for the life of the connection. All write to the master and
     * read from the nodes {@code readFrom} selects.
     *
     * @param clientName the Quarkus Redis client name, used for logging
     * @param clientResources shared client resources (with Vert.x event loops)
     * @param mode how the nodes are found
     * @param nodes the sentinel URI, or the Redis URI(s), as described above
     * @param clientOptions the client options, carrying the TLS material (see {@link LettuceClientSettings}); the
     *        command timeout options are added to them
     * @param readFrom the nodes read-only commands are sent to (see {@link LettuceClientSettings#readFrom})
     * @param timeout the {@code quarkus.redis.timeout} applied to non-blocking commands
     */
    public LettuceConnectionFactory(String clientName, ClientResources clientResources, MasterReplicaMode mode,
            List<RedisURI> nodes, ClientOptions clientOptions, ReadFrom readFrom, Duration timeout) {
        nonNull(mode, "mode");
        nonNull(nodes, "nodes");
        nonNull(readFrom, "readFrom");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("At least one node is required for the Redis client " + clientName);
        }
        RedisURI first = nodes.getFirst();
        if (mode == MasterReplicaMode.SENTINEL && (nodes.size() != 1 || first.getSentinels().isEmpty())) {
            throw new IllegalArgumentException("A single URI naming the sentinels is required for the Sentinel Redis client "
                    + clientName);
        }
        String tls = first.isSsl() ? " (TLS)" : "";
        switch (mode) {
            case SENTINEL -> LOGGER.infof(
                    "Creating Lettuce RedisClient '%s' for the master '%s' monitored by the sentinels %s%s, reading from %s",
                    clientName, first.getSentinelMasterId(), describe(first.getSentinels()), tls, describe(readFrom));
            case DISCOVER -> LOGGER.infof(
                    "Creating Lettuce RedisClient '%s' discovering the replication topology from %s%s, reading from %s",
                    clientName, describe(nodes), tls, describe(readFrom));
            case STATIC -> LOGGER.infof("Creating Lettuce RedisClient '%s' for the replication nodes %s%s, reading from %s",
                    clientName, describe(nodes), tls, describe(readFrom));
        }
        RedisClient client = RedisClient.create(clientResources);
        client.setOptions(withCommandTimeout(clientOptions, timeout));
        this.clientName = clientName;
        this.topology = new MasterReplica(client, mode, List.copyOf(nodes), readFrom);
    }

    private static ClientOptions withCommandTimeout(ClientOptions clientOptions, Duration timeout) {
        return clientOptions.mutate()
                .timeoutOptions(TimeoutOptions.builder()
                        .timeoutCommands(true)
                        .timeoutSource(new NonBlockingCommandTimeoutSource(timeout))
                        .build())
                .build();
    }

    /**
     * Names the read setting for the logs: the {@link ReadFrom} constants do not override {@code toString()}.
     */
    private static String describe(ReadFrom readFrom) {
        if (readFrom == ReadFrom.UPSTREAM) {
            return "the upstream nodes";
        }
        if (readFrom == ReadFrom.ANY) {
            return "any node";
        }
        if (readFrom == ReadFrom.REPLICA_PREFERRED) {
            return "the replicas, else the upstream node";
        }
        if (readFrom == ReadFrom.REPLICA) {
            return "the replicas";
        }
        return readFrom.getClass().getSimpleName();
    }

    private static String describe(List<RedisURI> nodes) {
        List<String> hosts = new ArrayList<>(nodes.size());
        for (RedisURI node : nodes) {
            hosts.add(node.getHost() + ":" + node.getPort());
        }
        return String.join(", ", hosts);
    }

    /**
     * Opens a new connection using the {@link ByteArrayCodec} codec: to the server, to the master and its replicas,
     * or to the cluster. {@link #connectAsync()} awaited, as Lettuce's own blocking {@code connect} methods await
     * their asynchronous counterparts; the connection of the Lettuce connection beans is opened this way at startup, on
     * the main thread (see {@link #connect(RedisCodec)}), the one of the data sources by their first command. An event loop
     * thread must use {@link #connectAsync()}: the await fails fast there instead of blocking the loop.
     *
     * @return a new {@link LettuceConnection}
     * @throws IllegalStateException when called on an event loop thread
     */
    public LettuceConnection connect() {
        return await(connectAsync());
    }

    /**
     * Opens a new connection asynchronously using the byte-array codec.
     * <p>
     * Unlike {@link #connect()}, this never blocks the calling thread and is therefore safe to
     * invoke from an event loop; the returned stage completes once the connection is established.
     *
     * @return a {@link CompletionStage} completing with a new {@link LettuceConnection}
     */
    public CompletionStage<LettuceConnection> connectAsync() {
        return topology.connectAsync();
    }

    /**
     * Opens a new connection using the given codec, as the Lettuce connection beans do: a
     * {@link io.lettuce.core.api.StatefulRedisConnection} to a standalone server, a
     * {@link StatefulRedisClusterConnection} to a cluster, a {@link StatefulRedisMasterReplicaConnection} to a
     * Sentinel-managed or replicated master. {@link #connectAsync(RedisCodec)} awaited, see {@link #connect()}.
     *
     * @throws IllegalStateException when called on an event loop thread
     */
    public <K, V> StatefulConnection<K, V> connect(RedisCodec<K, V> codec) {
        return await(connectAsync(codec));
    }

    /**
     * Opens a new connection asynchronously using the given codec, see {@link #connect(RedisCodec)}. Never blocks
     * the calling thread.
     */
    public <K, V> CompletionStage<StatefulConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
        return topology.connectAsync(codec);
    }

    /**
     * Awaits a connection being opened. A connection completes on an event loop thread, so awaiting one there could
     * block the loop that would complete it: the await is rejected on an event loop thread with a message naming the
     * client, as {@link LettuceResult#toBlocking} rejects blocking on a command result. The failure of the
     * connection attempt is rethrown as Lettuce's blocking {@code connect} methods rethrow it: a
     * {@link RedisException} as is, any other cause wrapped in a {@link RedisConnectionException}.
     */
    private <T> T await(CompletionStage<T> connecting) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException("The Lettuce Redis client '" + clientName
                    + "' cannot open a connection from an event loop thread: use connectAsync()");
        }
        try {
            return connecting.toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RedisException failure) {
                throw failure;
            }
            throw RedisConnectionException.create(e.getCause());
        }
    }

    /**
     * Opens a new Pub/Sub connection asynchronously using the byte-array codec. Each subscriber gets its own
     * connection: a connection in subscribed mode cannot run ordinary commands, so Pub/Sub connections are kept
     * apart from both the shared data source connection and the pool. Where it goes depends on the topology, see
     * {@link Topology#connectPubSubAsync()}.
     * <p>
     * Never blocks the calling thread and is therefore safe to invoke from an event loop.
     *
     * @return a {@link CompletionStage} completing with a new {@link StatefulRedisPubSubConnection}
     */
    public CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> connectPubSubAsync() {
        return topology.connectPubSubAsync();
    }

    /**
     * Returns the database every connection this factory opens starts on: the database index of the configured URI
     * ({@code 0} unless the URI names one) for a standalone server and for a master/replica connection (the one of
     * the first node), always {@code 0} for a cluster, which has no other database (Lettuce ignores the database of
     * the seed URIs; {@code LettuceClientSetup} warns about it).
     */
    public int getDatabase() {
        return topology.database();
    }

    /**
     * Whether the connections of this factory are to a cluster.
     */
    public boolean isCluster() {
        return topology instanceof Cluster;
    }

    /**
     * Returns the underlying Lettuce client: a {@link RedisClient} or a {@link RedisClusterClient}.
     */
    public AbstractRedisClient getClient() {
        return topology.client();
    }

    /**
     * Returns the underlying {@link RedisClient}: the one of a standalone server, or the one opening the
     * master/replica connections of a Sentinel or replication client.
     *
     * @throws IllegalStateException if this factory connects to a cluster
     */
    public RedisClient getRedisClient() {
        if (topology instanceof Standalone standalone) {
            return standalone.client();
        }
        if (topology instanceof MasterReplica masterReplica) {
            return masterReplica.client();
        }
        throw new IllegalStateException("This Lettuce client connects to a cluster, use getClusterClient()");
    }

    /**
     * Returns the underlying {@link RedisClusterClient}.
     *
     * @throws IllegalStateException if this factory connects to a standalone server
     */
    public RedisClusterClient getClusterClient() {
        if (topology instanceof Cluster cluster) {
            return cluster.client();
        }
        throw new IllegalStateException("This Lettuce client connects to a standalone server, use getRedisClient()");
    }

    /**
     * Shuts down the Lettuce client.
     * Must be called before shutting down the shared {@link ClientResources}.
     */
    public void shutdown() {
        LOGGER.infof("Shutting down Lettuce %s", topology.client().getClass().getSimpleName());
        topology.client().shutdown();
    }

    /**
     * How the connections of a client are opened, one implementation per client type: the Lettuce client, the
     * connections it opens with the byte-array codec for the data sources and the pool, and with a given codec for
     * the Lettuce beans, and the database every connection starts on.
     */
    private sealed interface Topology permits Standalone, Cluster, MasterReplica {

        AbstractRedisClient client();

        CompletionStage<LettuceConnection> connectAsync();

        <K, V> CompletionStage<StatefulConnection<K, V>> connectAsync(RedisCodec<K, V> codec);

        CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> connectPubSubAsync();

        int database();

    }

    /**
     * A standalone server: plain connections to {@code uri}, starting on its database.
     */
    private record Standalone(RedisClient client, RedisURI uri) implements Topology {

        @Override
        public CompletionStage<LettuceConnection> connectAsync() {
            return client.connectAsync(ByteArrayCodec.INSTANCE, uri).thenApply(LettuceConnection::standalone);
        }

        @Override
        public <K, V> CompletionStage<StatefulConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
            return client.connectAsync(codec, uri).thenApply(connection -> connection);
        }

        @Override
        public CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> connectPubSubAsync() {
            return client.connectPubSubAsync(ByteArrayCodec.INSTANCE, uri);
        }

        @Override
        public int database() {
            return uri.getDatabase();
        }

    }

    /**
     * A cluster: connections routing each command to the node owning its key and reading from the nodes
     * {@code readFrom} selects. A cluster only has database {@code 0}.
     */
    private record Cluster(RedisClusterClient client, ReadFrom readFrom, AtomicReference<CompletionStage<Void>> partitions)
            implements
                Topology {

        Cluster(RedisClusterClient client, ReadFrom readFrom) {
            this(client, readFrom, new AtomicReference<>());
        }

        @Override
        public CompletionStage<LettuceConnection> connectAsync() {
            return connectCluster(ByteArrayCodec.INSTANCE).thenApply(LettuceConnection::cluster);
        }

        @Override
        public <K, V> CompletionStage<StatefulConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
            return connectCluster(codec).thenApply(connection -> connection);
        }

        @Override
        public CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> connectPubSubAsync() {
            return partitionsLoaded().thenCompose(ignored -> client.connectPubSubAsync(ByteArrayCodec.INSTANCE))
                    .thenApply(connection -> connection);
        }

        /**
         * Opens a cluster connection reading from the nodes {@code readFrom} selects. Lettuce's asynchronous connects
         * require the partitions to be loaded first, which only its blocking connect does on its own, so they are
         * loaded before the first connection (see {@link #partitionsLoaded()}).
         */
        private <K, V> CompletionStage<StatefulRedisClusterConnection<K, V>> connectCluster(RedisCodec<K, V> codec) {
            return partitionsLoaded().thenCompose(ignored -> client.connectAsync(codec)).thenApply(connection -> {
                connection.setReadFrom(readFrom);
                return connection;
            });
        }

        /**
         * The partitions, loaded from the seed nodes once, when the first connection is opened, and loaded again after
         * a failed attempt so that a cluster that was unreachable can be reached later. Lettuce refreshes them itself
         * afterwards, as the topology refresh options say.
         */
        private CompletionStage<Void> partitionsLoaded() {
            CompletionStage<Void> loaded = partitions.get();
            if (loaded != null) {
                return loaded;
            }
            CompletableFuture<Void> loading = new CompletableFuture<>();
            if (!partitions.compareAndSet(null, loading)) {
                return partitions.get();
            }
            client.refreshPartitionsAsync().whenComplete((ignored, failure) -> {
                if (failure != null) {
                    partitions.compareAndSet(loading, null);
                    loading.completeExceptionally(failure);
                } else {
                    loading.complete(null);
                }
            });
            return loading;
        }

        @Override
        public int database() {
            return 0;
        }

    }

    /**
     * A master and its replicas, managed by Redis Sentinel or addressed directly: master/replica connections built by
     * Lettuce's {@code io.lettuce.core.masterreplica.MasterReplica} (named in full, as this record takes its name)
     * from the single sentinel URI, from the Redis URI to discover the topology from, or from the Redis URIs of the
     * nodes, as {@code mode} says (Lettuce decides between its sentinel, autodiscovery and static connectors by the
     * shape of what it is given: a single Redis URI means autodiscovery to it, so the mode, not the number of nodes,
     * picks the overload). The connections write to the master and read from the nodes {@code readFrom} selects.
     * They are {@link io.lettuce.core.api.StatefulRedisConnection}s, so they take the standalone path of
     * {@link LettuceConnection}. The database is the one of the first node.
     */
    private record MasterReplica(RedisClient client, MasterReplicaMode mode, List<RedisURI> nodes, ReadFrom readFrom)
            implements
                Topology {

        @Override
        public CompletionStage<LettuceConnection> connectAsync() {
            return connectMasterReplicaAsync(ByteArrayCodec.INSTANCE).thenApply(LettuceConnection::standalone);
        }

        @Override
        public <K, V> CompletionStage<StatefulConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
            return connectMasterReplicaAsync(codec).thenApply(connection -> connection);
        }

        @Override
        public CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> connectPubSubAsync() {
            // Lettuce has no master/replica Pub/Sub connection: a plain one is opened instead. A sentinel URI makes
            // the client look the master up through the sentinels; the Redis URIs are tried in order.
            return mode == MasterReplicaMode.SENTINEL
                    ? client.connectPubSubAsync(ByteArrayCodec.INSTANCE, nodes.getFirst())
                    // As a plain future: Lettuce's ConnectionFuture does not compose correctly through
                    // exceptionallyCompose.
                    : tryInOrder(0, node -> client.connectPubSubAsync(ByteArrayCodec.INSTANCE, node).toCompletableFuture(),
                            "open a Pub/Sub connection to");
        }

        @Override
        public int database() {
            return nodes.getFirst().getDatabase();
        }

        private <K, V> CompletionStage<StatefulRedisMasterReplicaConnection<K, V>> connectMasterReplicaAsync(
                RedisCodec<K, V> codec) {
            CompletionStage<StatefulRedisMasterReplicaConnection<K, V>> connecting = switch (mode) {
                case SENTINEL -> io.lettuce.core.masterreplica.MasterReplica.connectAsync(client, codec, nodes.getFirst());
                case DISCOVER -> discoverAsync(codec);
                case STATIC -> io.lettuce.core.masterreplica.MasterReplica.connectAsync(client, codec, nodes);
            };
            return connecting.thenApply(connection -> {
                connection.setReadFrom(readFrom);
                return connection;
            });
        }

        /**
         * Discovers the topology from the first node that answers, trying the nodes in order as the Vert.x client
         * does (Lettuce's Autodiscovery connector takes a single node). The failure of the last attempt is reported,
         * the earlier ones suppressed on it.
         */
        private <K, V> CompletionStage<StatefulRedisMasterReplicaConnection<K, V>> discoverAsync(RedisCodec<K, V> codec) {
            return tryInOrder(0, node -> io.lettuce.core.masterreplica.MasterReplica.connectAsync(client, codec, node),
                    "discover the replication topology from");
        }

        /**
         * Runs {@code attempt} on {@code nodes[index]}, falling back to the next node when it fails. The failure of
         * the last attempt is reported, the earlier ones suppressed on it.
         *
         * @param what what the attempt does, for the log line when a node is skipped ("<what> host:port")
         */
        private <T> CompletionStage<T> tryInOrder(int index, Function<RedisURI, CompletionStage<T>> attempt, String what) {
            RedisURI node = nodes.get(index);
            CompletionStage<T> attempting = attempt.apply(node);
            if (index == nodes.size() - 1) {
                return attempting;
            }
            return attempting.exceptionallyCompose(failure -> {
                logNextNode(node, failure, what);
                return tryInOrder(index + 1, attempt, what).whenComplete((connection, next) -> {
                    if (next != null) {
                        unwrap(next).addSuppressed(unwrap(failure));
                    }
                });
            });
        }

        private static void logNextNode(RedisURI node, Throwable failure, String what) {
            LOGGER.warnf("Unable to %s %s:%d (%s), trying the next host", what, node.getHost(), node.getPort(),
                    unwrap(failure).getMessage());
        }

        private static Throwable unwrap(Throwable failure) {
            return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
        }

    }

    /**
     * Applies the command timeout to every command but the ones bounded otherwise: the commands of a transaction, and
     * the blocking commands (see {@link BlockingCommands}), whether typed or sent through {@code execute(...)}.
     */
    static final class NonBlockingCommandTimeoutSource extends TimeoutOptions.TimeoutSource {

        private final long timeoutMillis;

        NonBlockingCommandTimeoutSource(Duration timeout) {
            this.timeoutMillis = timeout.toMillis();
        }

        @Override
        public long getTimeout(RedisCommand<?, ?, ?> command) {
            if (command instanceof TransactionalCommand<?, ?, ?>) {
                return -1; // completes on EXEC, bounded by the EXEC timeout
            }
            ProtocolKeyword type = command.getType();
            if (BlockingCommands.alwaysBlocks(type)) {
                return -1; // bounded by its own timeout
            }
            if (BlockingCommands.blocksWithBlockOption(type) && BlockingReads.isMarked(command.getArgs())) {
                return -1; // bounded by its own BLOCK duration
            }
            return timeoutMillis;
        }

    }

}

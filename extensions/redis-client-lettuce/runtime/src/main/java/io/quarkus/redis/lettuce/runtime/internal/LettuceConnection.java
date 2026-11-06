package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

/**
 * A byte-array connection to a standalone Redis server or to a Redis cluster, as opened by
 * {@link LettuceConnectionFactory}: the topology-neutral handle the data sources, the command groups and the
 * {@link LettuceConnectionPool} work with.
 * <p>
 * Both Lettuce connection types expose the data structure, server and raw dispatch commands through
 * {@link RedisClusterAsyncCommands}, which the standalone {@link RedisAsyncCommands} extends; {@link #async()}
 * returns that shared surface, so the command groups are written once and the cluster connection routes each
 * command to the node owning its key. What the two topologies do not share is wrapped here:
 * <ul>
 * <li>a cluster has no transactions. Redis does not support {@code MULTI}/{@code EXEC}/{@code WATCH} across nodes
 * and Lettuce does not route them, so {@link #multi()}, {@link #exec()}, {@link #discard()}, {@link #watch(byte[]...)}
 * and {@link #unwatch()} fail at once with an {@link UnsupportedOperationException} on a cluster connection. The data
 * sources check {@link #isCluster()} before they borrow a connection for a transaction, so that message is what
 * {@code withTransaction} surfaces;</li>
 * <li>a cluster has a single database. {@link #select(int)} is sent as a raw command for the server to reject, as
 * the Vert.x client does, and the pool skips the database reset for cluster connections.</li>
 * </ul>
 */
public final class LettuceConnection implements AutoCloseable {

    /**
     * The message of the failure {@code withTransaction} and the transaction commands raise on a cluster.
     */
    public static final String TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER = "Transactions (MULTI/EXEC/WATCH) are not supported "
            + "on a Redis cluster by the Lettuce backend: Redis does not support them across cluster nodes and Lettuce does "
            + "not route them. Use a standalone client, or the Vert.x backend with "
            + "quarkus.redis.cluster-transactions=single-node.";

    private final StatefulConnection<byte[], byte[]> connection;
    private final RedisClusterAsyncCommands<byte[], byte[]> async;
    /** The standalone command surface, carrying the transaction and database commands; {@code null} on a cluster. */
    private final RedisAsyncCommands<byte[], byte[]> standalone;

    private LettuceConnection(StatefulConnection<byte[], byte[]> connection, RedisClusterAsyncCommands<byte[], byte[]> async,
            RedisAsyncCommands<byte[], byte[]> standalone) {
        this.connection = connection;
        this.async = async;
        this.standalone = standalone;
    }

    /**
     * Wraps a connection to a standalone server.
     */
    public static LettuceConnection standalone(StatefulRedisConnection<byte[], byte[]> connection) {
        nonNull(connection, "connection");
        RedisAsyncCommands<byte[], byte[]> async = connection.async();
        return new LettuceConnection(connection, async, async);
    }

    /**
     * Wraps a connection to a cluster.
     */
    public static LettuceConnection cluster(StatefulRedisClusterConnection<byte[], byte[]> connection) {
        nonNull(connection, "connection");
        return new LettuceConnection(connection, connection.async(), null);
    }

    /**
     * The commands available on both topologies: every command but the transaction and database ones.
     */
    public RedisClusterAsyncCommands<byte[], byte[]> async() {
        return async;
    }

    /**
     * Whether this connection is to a cluster, where transactions and database selection are not available.
     */
    public boolean isCluster() {
        return standalone == null;
    }

    /**
     * The wrapped Lettuce connection: a {@link StatefulRedisConnection} or a {@link StatefulRedisClusterConnection}.
     */
    public StatefulConnection<byte[], byte[]> connection() {
        return connection;
    }

    /**
     * Issues {@code SELECT}. On a cluster the command is dispatched as is, for the server to reject it as it does for
     * the Vert.x client: a cluster only has database {@code 0}.
     */
    public RedisFuture<String> select(int db) {
        if (standalone != null) {
            return standalone.select(db);
        }
        return async.dispatch(CommandType.SELECT, new StatusOutput<>(ByteArrayCodec.INSTANCE),
                new CommandArgs<>(ByteArrayCodec.INSTANCE).add(db));
    }

    /**
     * Issues {@code MULTI}.
     *
     * @throws UnsupportedOperationException on a cluster connection
     */
    public RedisFuture<String> multi() {
        return transactional().multi();
    }

    /**
     * Issues {@code EXEC}.
     *
     * @throws UnsupportedOperationException on a cluster connection
     */
    public RedisFuture<TransactionResult> exec() {
        return transactional().exec();
    }

    /**
     * Issues {@code DISCARD}.
     *
     * @throws UnsupportedOperationException on a cluster connection
     */
    public RedisFuture<String> discard() {
        return transactional().discard();
    }

    /**
     * Issues {@code WATCH}.
     *
     * @throws UnsupportedOperationException on a cluster connection
     */
    public RedisFuture<String> watch(byte[]... keys) {
        return transactional().watch(keys);
    }

    /**
     * Issues {@code UNWATCH}.
     *
     * @throws UnsupportedOperationException on a cluster connection
     */
    public RedisFuture<String> unwatch() {
        return transactional().unwatch();
    }

    private RedisAsyncCommands<byte[], byte[]> transactional() {
        if (standalone == null) {
            throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER);
        }
        return standalone;
    }

    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public void close() {
        connection.close();
    }

    public CompletableFuture<Void> closeAsync() {
        return connection.closeAsync();
    }

}

package io.quarkus.redis.lettuce.runtime.internal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.netty.channel.EventLoopGroup;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceBlockingRedisDataSourceImpl;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.vertx.core.internal.VertxInternal;
import io.vertx.mutiny.core.Vertx;

public abstract class CommandsTestBase {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);
    protected static final String REDIS_DEFAULT_IMAGE = "redis:8-alpine";
    protected static final GenericContainer<?> REDIS = createContainer();
    protected static final int MAX_POOL_SIZE = 6;
    protected static final int MAX_POOL_WAITING = 24;

    protected static Vertx vertx;
    protected static LettuceClientResources lettuceResources;
    protected static RedisClient redisClient;
    protected static RedisURI redisUri;
    protected static RedisCodec<byte[], byte[]> codec;
    protected static StatefulRedisConnection<byte[], byte[]> connection;
    /** {@link #connection} as the data sources see it. */
    protected static LettuceConnection lettuceConnection;

    protected final String key = UUID.randomUUID().toString();

    @BeforeAll
    static void setUp() {
        if (REDIS.isRunning()) {
            return;
        }
        REDIS.start();

        vertx = Vertx.vertx();
        EventLoopGroup loops = ((VertxInternal) vertx.getDelegate()).eventLoopGroup();
        lettuceResources = new LettuceClientResources(loops);

        redisUri = RedisURI.create(REDIS.getHost(), REDIS.getFirstMappedPort());
        redisClient = RedisClient.create(lettuceResources.clientResources(), redisUri);
        codec = ByteArrayCodec.INSTANCE;
        connection = redisClient.connect(codec);
        lettuceConnection = LettuceConnection.standalone(connection);
    }

    @AfterAll
    static void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (lettuceResources != null) {
            lettuceResources.shutdown();
        }
        if (vertx != null) {
            vertx.closeAndAwait();
        }
        if (REDIS.isRunning()) {
            REDIS.stop();
        }
    }

    @BeforeEach
    void flush() {
        connection.sync().flushall();
    }

    private static GenericContainer<?> createContainer() {
        GenericContainer<?> container = new GenericContainer<>(
                DockerImageName.parse(System.getProperty("redis.base.image", REDIS_DEFAULT_IMAGE)));
        container.withExposedPorts(6379);
        // The mapped port can accept TCP connections before Redis serves RESP; wait for the server's ready log line.
        container.waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
        return container;
    }

    protected static CompletionStage<LettuceConnection> connectAsync() {
        return redisClient.connectAsync(codec, redisUri).thenApply(LettuceConnection::standalone);
    }

    protected static Supplier<CompletionStage<LettuceConnection>> connector() {
        return CommandsTestBase::connectAsync;
    }

    /** The sync API of a standalone {@link LettuceConnection}, for the raw checks of the tests. */
    protected static RedisCommands<byte[], byte[]> sync(LettuceConnection connection) {
        return ((StatefulRedisConnection<byte[], byte[]>) connection.connection()).sync();
    }

    protected static Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector() {
        return () -> redisClient.connectPubSubAsync(codec, redisUri);
    }

    protected static LettuceConnectionPool pool() {
        return pool(MAX_POOL_SIZE, MAX_POOL_WAITING);
    }

    protected static LettuceConnectionPool pool(int maxPoolSize, int maxWaiting) {
        return new LettuceConnectionPool(connector(), maxPoolSize, maxWaiting, redisUri.getDatabase());
    }

    protected static LettuceReactiveRedisDataSourceImpl reactiveDataSource() {
        return new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool(), pubSubConnector());
    }

    protected static LettuceBlockingRedisDataSourceImpl blockingDataSource() {
        return blockingDataSource(TIMEOUT);
    }

    protected static LettuceBlockingRedisDataSourceImpl blockingDataSource(Duration timeout) {
        return new LettuceBlockingRedisDataSourceImpl(reactiveDataSource(), timeout);
    }

    protected static long connectionCount() {
        String list = connection.sync().clientList();
        return list.isEmpty() ? 0 : list.split("\n").length;
    }

    protected static void rawSet(String key, String value) {
        connection.sync().set(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }

    protected static String rawGet(String key) {
        byte[] bytes = connection.sync().get(key.getBytes(StandardCharsets.UTF_8));
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    protected static String rawGetOnDatabase(int database, String key) {
        connection.sync().select(database);
        try {
            return rawGet(key);
        } finally {
            connection.sync().select(0);
        }
    }

    protected static String lastCommandOf(long clientId) {
        for (String line : connection.sync().clientList().split("\n")) {
            if (line.startsWith("id=" + clientId + " ")) {
                for (String field : line.split(" ")) {
                    if (field.startsWith("cmd=")) {
                        return field.substring("cmd=".length());
                    }
                }
            }
        }
        throw new AssertionError("No client with id " + clientId + " in CLIENT LIST");
    }

}

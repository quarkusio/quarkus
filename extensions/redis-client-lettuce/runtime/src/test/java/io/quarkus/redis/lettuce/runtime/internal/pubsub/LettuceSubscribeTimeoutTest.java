package io.quarkus.redis.lettuce.runtime.internal.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands.ReactiveRedisSubscriber;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionFactory;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;

/**
 * Exercises {@code quarkus.redis.subscribe-timeout}: a subscription the server does not confirm in time fails and its
 * connection is closed, and the command timeout does not apply to it.
 * <p>
 * The server is kept from confirming with {@code CLIENT PAUSE}, issued once the Pub/Sub connection is open: a paused
 * server would not answer the handshake of a new connection either.
 */
class LettuceSubscribeTimeoutTest extends CommandsTestBase {

    private static final Duration SHORT = Duration.ofMillis(300);
    private static final long PAUSE_MILLIS = 2000;

    private LettuceConnectionFactory factory;
    private LettuceConnection conn;
    private LettuceConnectionPool pool;
    private LettuceReactiveRedisDataSourceImpl ds;
    private final AtomicReference<StatefulRedisPubSubConnection<byte[], byte[]>> pubSubConnection = new AtomicReference<>();

    private void connect(Duration commandTimeout, Duration subscribeTimeout) {
        factory = new LettuceConnectionFactory("subscribe-timeout-test", lettuceResources.clientResources(),
                RedisURI.create(REDIS.getHost(), REDIS.getFirstMappedPort()), ClientOptions.create(), commandTimeout,
                subscribeTimeout);
        conn = factory.connect();
        pool = new LettuceConnectionPool(factory::connectAsync, 1, 1, 0);
        // pauses the server as soon as the connection of the subscriber is open
        LettuceReactiveRedisDataSourceImpl admin = new LettuceReactiveRedisDataSourceImpl(vertx, conn, pool,
                factory::connectPubSubAsync);
        ds = new LettuceReactiveRedisDataSourceImpl(vertx, conn, pool, () -> pausedAfterConnect(admin));
    }

    private CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>> pausedAfterConnect(
            LettuceReactiveRedisDataSourceImpl admin) {
        return factory.connectPubSubAsync().thenCompose(connection -> {
            pubSubConnection.set(connection);
            return admin.execute("CLIENT", "PAUSE", Long.toString(PAUSE_MILLIS)).subscribeAsCompletionStage()
                    .thenApply(ignored -> connection);
        });
    }

    @AfterEach
    void shutdown() {
        // let the pause elapse, or it would hold the commands of the next test
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(ds.execute("PING").await().atMost(TIMEOUT).toString())
                .isEqualTo("PONG"));
        pool.close().await().atMost(TIMEOUT);
        conn.close();
        factory.shutdown();
    }

    @Test
    void anUnconfirmedSubscriptionFailsAfterTheSubscribeTimeoutAndItsConnectionIsClosed() {
        // the command timeout is far longer than the pause: only the subscribe timeout can fail the subscription
        connect(Duration.ofSeconds(10), SHORT);

        long start = System.nanoTime();
        assertThatThrownBy(() -> ds.pubsub(String.class).subscribe("subscribe-timeout", message -> {
        }).await().atMost(TIMEOUT))
                .hasCauseInstanceOf(TimeoutException.class)
                .hasRootCauseInstanceOf(RedisCommandTimeoutException.class)
                .hasMessageContaining("No subscription confirmation received from Redis");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(PAUSE_MILLIS));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(pubSubConnection.get().isOpen()).isFalse());
    }

    @Test
    void theCommandTimeoutDoesNotApplyToASubscription() {
        // no subscribe timeout, and a command timeout far shorter than the pause
        connect(SHORT, Duration.ZERO);
        List<String> received = new CopyOnWriteArrayList<>();

        long start = System.nanoTime();
        ReactiveRedisSubscriber subscriber = ds.pubsub(String.class).subscribe("subscribe-timeout", received::add)
                .await().atMost(TIMEOUT);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThan(SHORT.multipliedBy(2));

        try {
            await().atMost(TIMEOUT).untilAsserted(() -> {
                ds.pubsub(String.class).publish("subscribe-timeout", "hello").await().atMost(TIMEOUT);
                assertThat(received).contains("hello");
            });
        } finally {
            subscriber.unsubscribe().await().atMost(TIMEOUT);
        }
    }
}

package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Response;

class LettuceBlockingCommandsPoolIntegrationTest extends CommandsTestBase {

    @Test
    void blockingCommandDoesNotHeadOfLineBlockOrdinaryCommands() {
        LettuceReactiveRedisDataSourceImpl ds = reactiveDataSource();
        String missingKey = UUID.randomUUID().toString();

        AtomicReference<KeyValue<String, String>> blpopResult = new AtomicReference<>();
        Cancellable blpop = ds.list(String.class, String.class)
                .blpop(Duration.ofSeconds(5), missingKey)
                .subscribe().with(blpopResult::set, t -> {
                });

        // Without the pool, this get() would sit in the socket buffer behind the pending BLPOP
        // until it times out. With the pool, it runs on the shared connection immediately.
        long start = System.nanoTime();
        ds.value(String.class, String.class).set(key, "v").await().atMost(Duration.ofSeconds(1));
        String value = ds.value(String.class, String.class).get(key).await().atMost(Duration.ofSeconds(1));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(value).isEqualTo("v");
        assertThat(elapsedMillis).isLessThan(1000);

        // Unblock the still-pending BLPOP so it doesn't leak past the test.
        rawPush(missingKey, "unblocked");
        await().atMost(TIMEOUT).until(() -> blpopResult.get() != null);
        assertThat(blpopResult.get()).isEqualTo(KeyValue.of(missingKey, "unblocked"));
        blpop.cancel();
    }

    @Test
    void rawBlockingCommandDoesNotHeadOfLineBlockOrdinaryCommands() {
        LettuceReactiveRedisDataSourceImpl ds = reactiveDataSource();
        String missingKey = UUID.randomUUID().toString();

        AtomicReference<Response> blpopResult = new AtomicReference<>();
        ds.execute("BLPOP", missingKey, "5").subscribe().with(blpopResult::set, t -> {
        });
        // Sent through execute(...), the command borrows a pooled connection for its duration, as the typed one does.
        await().atMost(TIMEOUT).until(() -> ds.getPool().getObjectCount() == 1 && ds.getPool().getIdle() == 0);

        // On the shared connection, these would wait behind the pending BLPOP.
        ds.value(String.class, String.class).set(key, "v").await().atMost(Duration.ofSeconds(1));
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(Duration.ofSeconds(1))).isEqualTo("v");

        rawPush(missingKey, "unblocked");
        await().atMost(TIMEOUT).until(() -> blpopResult.get() != null);
        assertThat(blpopResult.get().get(0).toString()).isEqualTo(missingKey);
        assertThat(blpopResult.get().get(1).toString()).isEqualTo("unblocked");
        await().atMost(TIMEOUT).until(() -> ds.getPool().getIdle() == 1);
    }

    @Test
    void rawStreamReadBorrowsAPooledConnectionOnlyWhenItBlocks() {
        LettuceReactiveRedisDataSourceImpl ds = reactiveDataSource();
        String stream = UUID.randomUUID().toString();

        // without BLOCK the read returns at once, and stays on the shared connection
        assertThat(ds.execute("XREAD", "COUNT", "1", "STREAMS", stream, "0-0").await().atMost(TIMEOUT)).isNull();
        assertThat(ds.getPool().getObjectCount()).isZero();

        AtomicBoolean read = new AtomicBoolean();
        ds.execute("XREAD", "BLOCK", "5000", "STREAMS", stream, "$").subscribe().with(entries -> read.set(true), t -> {
        });
        await().atMost(TIMEOUT).until(() -> ds.getPool().getObjectCount() == 1 && ds.getPool().getIdle() == 0);
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(Duration.ofSeconds(1))).isNull();

        connection.sync().xadd(stream.getBytes(StandardCharsets.UTF_8),
                Map.of("field".getBytes(StandardCharsets.UTF_8), "value".getBytes(StandardCharsets.UTF_8)));
        await().atMost(TIMEOUT).until(read::get);
        await().atMost(TIMEOUT).until(() -> ds.getPool().getIdle() == 1);
    }

    @Test
    void rawBlockingCommandOnAPinnedDataSourceRunsOnItsConnection() {
        // One connection and no queue: borrowing a second connection inside the scope would fail at once.
        LettuceConnectionPool pool = pool(1, 0);
        LettuceReactiveRedisDataSourceImpl ds = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool,
                pubSubConnector());
        String k = UUID.randomUUID().toString();
        rawPush(k, "v");
        AtomicReference<Response> popped = new AtomicReference<>();

        ds.withConnection(pinned -> pinned.execute("BLPOP", k, "1").invoke(popped::set).replaceWithVoid())
                .await().atMost(TIMEOUT);

        assertThat(popped.get().get(1).toString()).isEqualTo("v");
        assertThat(pool.getObjectCount()).isEqualTo(1);
    }

    @Test
    void commandsTakingTheirConnectionOverAreRejectedByExecute() {
        LettuceReactiveRedisDataSourceImpl ds = reactiveDataSource();

        for (String command : List.of("SUBSCRIBE", "psubscribe", "SSubscribe")) {
            assertThatThrownBy(() -> ds.execute(command, "channel"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(command.toUpperCase())
                    .hasMessageContaining("pubsub(...)");
        }
        assertThatThrownBy(() -> ds.execute(Command.MONITOR))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("MONITOR");
        // on a pinned connection too: it goes back to the pool when the scope ends
        assertThatThrownBy(() -> ds.withConnection(pinned -> pinned.execute("SUBSCRIBE", "channel").replaceWithVoid())
                .await().atMost(TIMEOUT))
                .isInstanceOf(UnsupportedOperationException.class);
        // and in a transaction: the command would be queued, and take the pooled connection over when EXEC runs it
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.execute("SUBSCRIBE", "channel")).await().atMost(TIMEOUT))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("SUBSCRIBE");
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.execute(Command.PSUBSCRIBE, "channel*")).await()
                .atMost(TIMEOUT))
                .isInstanceOf(UnsupportedOperationException.class);
        // no pooled connection was left subscribed
        assertThat(connection.sync().clientList()).doesNotContain("flags=P");

        // the shared connection still answers commands
        assertThat(ds.execute("PING").await().atMost(TIMEOUT).toString()).isEqualTo("PONG");
    }

    @Test
    void concurrentBlockingCallsAreBoundedByThePoolAndQueueBeyondMaxPoolSize() {
        LettuceConnectionPool pool = pool(2, 1);
        LettuceReactiveRedisDataSourceImpl ds = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool,
                pubSubConnector());

        String keyA = UUID.randomUUID().toString();
        String keyB = UUID.randomUUID().toString();
        String keyC = UUID.randomUUID().toString();

        // Two concurrent BLPOPs saturate the pool (maxPoolSize=2); both stay pending.
        AtomicReference<Object> resultA = new AtomicReference<>();
        AtomicReference<Object> resultB = new AtomicReference<>();
        ds.list(String.class, String.class).blpop(Duration.ofSeconds(5), keyA).subscribe().with(resultA::set, t -> {
        });
        ds.list(String.class, String.class).blpop(Duration.ofSeconds(5), keyB).subscribe().with(resultB::set, t -> {
        });
        assertThat(resultA.get()).isNull();
        assertThat(resultB.get()).isNull();

        // A third caller queues (maxWaiting=1) instead of failing.
        AtomicReference<Object> resultC = new AtomicReference<>();
        ds.list(String.class, String.class).blpop(Duration.ofSeconds(5), keyC).subscribe().with(resultC::set, t -> {
        });
        assertThat(resultC.get()).isNull();

        // A fourth caller is rejected immediately: the waiting queue is also bounded.
        assertThatThrownBy(() -> ds.list(String.class, String.class).blpop(Duration.ofSeconds(5), keyC)
                .await().atMost(TIMEOUT))
                .hasMessageContaining("too many waiting requests");

        // Unblocking A's BLPOP frees its pooled connection, which is handed straight to the queued
        // caller C — whose own BLPOP (on keyC) only then starts running on that connection.
        rawPush(keyA, "for-a");
        await().atMost(TIMEOUT).until(() -> resultA.get() != null);
        assertThat(resultA.get()).isEqualTo(KeyValue.of(keyA, "for-a"));

        rawPush(keyC, "for-c");
        await().atMost(TIMEOUT).until(() -> resultC.get() != null);
        assertThat(resultC.get()).isEqualTo(KeyValue.of(keyC, "for-c"));

        rawPush(keyB, "for-b");
        await().atMost(TIMEOUT).until(() -> resultB.get() != null);
        assertThat(resultB.get()).isEqualTo(KeyValue.of(keyB, "for-b"));
    }

    @Test
    void invalidBlockingArgumentsFailAtCallTimeWithoutTouchingThePool() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceReactiveRedisDataSourceImpl ds = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool,
                pubSubConnector());

        // Argument validation runs eagerly, so the call itself throws — no subscription needed —
        // matching the behaviour of non-blocking commands.
        assertThatThrownBy(() -> ds.list(String.class, String.class).blpop(Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keys");
        assertThatThrownBy(() -> ds.list(String.class, String.class).blpop(Duration.ofSeconds(1), (String) null))
                .isInstanceOf(IllegalArgumentException.class);

        // No connection was acquired for the rejected calls.
        assertThat(pool.getObjectCount()).isZero();

        // The pool is fully usable afterwards: a valid BLPOP on the same 1-connection pool completes.
        String k = UUID.randomUUID().toString();
        rawPush(k, "v");
        KeyValue<String, String> result = ds.list(String.class, String.class).blpop(Duration.ofSeconds(1), k)
                .await().atMost(TIMEOUT);
        assertThat(result).isEqualTo(KeyValue.of(k, "v"));
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
    }

    private static void rawPush(String key, String value) {
        connection.sync().rpush(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }

}

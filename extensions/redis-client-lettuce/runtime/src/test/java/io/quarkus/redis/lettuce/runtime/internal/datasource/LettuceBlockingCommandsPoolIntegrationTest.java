package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.smallrye.mutiny.subscription.Cancellable;

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

package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.smallrye.mutiny.Uni;

class LettuceWithConnectionBlockingIntegrationTest extends CommandsTestBase {

    RedisDataSource ds;

    @BeforeEach
    void initialize() {
        ds = blockingDataSource();
    }

    @Test
    void runsBlockOnPinnedConnection() {
        AtomicLong captured = new AtomicLong();
        ds.withConnection(rds -> captured.set(rds.execute("CLIENT", "ID").toLong()));
        long sharedId = connection.sync().clientId();
        assertThat(captured.get()).isPositive().isNotEqualTo(sharedId);
    }

    @Test
    void clientIdStableWithinBlock_reusedAcrossSequentialBlocks() {
        // Proves pooling, not connect-per-call: sequential withConnection calls borrow from a pool
        // of one idle connection, so the second block gets the exact connection the first released.
        AtomicLong first = new AtomicLong();
        AtomicLong second = new AtomicLong();
        ds.withConnection(rds -> {
            long a = rds.execute("CLIENT", "ID").toLong();
            long b = rds.execute("CLIENT", "ID").toLong();
            assertThat(a).isEqualTo(b);
            first.set(a);
        });
        ds.withConnection(rds -> second.set(rds.execute("CLIENT", "ID").toLong()));
        assertThat(second.get()).isEqualTo(first.get());
    }

    @Test
    void nestedWithConnectionReusesOuterConnection() {
        AtomicLong outerId = new AtomicLong();
        AtomicLong innerId = new AtomicLong();
        ds.withConnection(outer -> {
            outerId.set(outer.execute("CLIENT", "ID").toLong());
            outer.withConnection(inner -> innerId.set(inner.execute("CLIENT", "ID").toLong()));
        });
        assertThat(innerId.get()).isEqualTo(outerId.get());
    }

    @Test
    void releasesConnectionToPoolOnFailure() {
        assertThatThrownBy(() -> ds.withConnection(rds -> {
            throw new RuntimeException("boom");
        })).hasMessageContaining("boom");
        LettuceConnectionPool pool = ((LettuceReactiveRedisDataSourceImpl) ds.getReactive()).getPool();
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == pool.getObjectCount());
    }

    @Test
    void releaseFailureDoesNotHideTheBlocksOwnException() {
        RedisDataSource failingRelease = withRelease(
                Uni.createFrom().failure(new IllegalStateException("release failed")), TIMEOUT);
        assertThatThrownBy(() -> failingRelease.withConnection(rds -> {
            throw new RuntimeException("boom");
        }))
                .hasMessage("boom")
                .satisfies(t -> {
                    assertThat(t.getSuppressed()).hasSize(1);
                    assertThat(t.getSuppressed()[0]).isInstanceOf(IllegalStateException.class).hasMessage("release failed");
                });
    }

    @Test
    void releaseTimeoutDoesNotHideTheBlocksOwnException() {
        RedisDataSource hangingRelease = withRelease(Uni.createFrom().nothing(), Duration.ofSeconds(1));
        assertThatThrownBy(() -> hangingRelease.withConnection(rds -> {
            throw new RuntimeException("boom");
        }))
                .hasMessage("boom")
                .satisfies(t -> assertThat(t.getSuppressed()).singleElement().isInstanceOf(TimeoutException.class));
    }

    @Test
    void releaseFailureAfterSuccessfulBlockIsLoggedNotThrown() {
        RedisDataSource failingRelease = withRelease(
                Uni.createFrom().failure(new IllegalStateException("release failed")), TIMEOUT);
        AtomicBoolean ran = new AtomicBoolean();
        // The block did its work: a failure to give the connection back is not the caller's failure.
        failingRelease.withConnection(rds -> ran.set(true));
        assertThat(ran).isTrue();
    }

    /** A data source whose connection release yields {@code release} instead of returning to the pool. */
    private static RedisDataSource withRelease(Uni<Void> release, Duration timeout) {
        LettuceReactiveRedisDataSourceImpl reactive = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
                pool(), pubSubConnector()) {
            @Override
            Uni<Void> releaseConnection(LettuceConnection conn) {
                return release;
            }
        };
        return new LettuceBlockingRedisDataSourceImpl(reactive, timeout);
    }

    @Test
    void thousandIterationsDoNotLeakConnections() {
        long before = connectionCount();
        for (int i = 0; i < 1000; i++) {
            ds.withConnection(rds -> rds.value(String.class, Integer.class).incr(key));
        }
        assertThat(ds.value(String.class, Integer.class).get(key)).isEqualTo(1000);
        await().atMost(TIMEOUT).until(() -> connectionCount() <= before + 1);
    }

    @Test
    void selectInsideBlockIsResetBeforeTheConnectionIsReused() {
        ds.withConnection(rds -> {
            rds.select(1);
            rds.value(String.class).set(key, "db1");
        });
        assertNextBorrowIsOnDefaultDatabase();
    }

    @Test
    void selectViaExecuteInsideBlockIsResetBeforeTheConnectionIsReused() {
        ds.withConnection(rds -> {
            rds.execute("SELECT", "1");
            rds.value(String.class).set(key, "db1");
        });
        assertNextBorrowIsOnDefaultDatabase();
    }

    @Test
    void selectBeforeNestedTransactionIsResetWhenTheOuterBlockReleases() {
        // The nested transaction pins a second data source to the same connection; the reset must
        // still happen when the outer block, which owns the connection, releases it.
        ds.withConnection(outer -> {
            outer.select(1);
            outer.withTransaction(tx -> tx.value(String.class).set(key, "db1"));
        });
        assertNextBorrowIsOnDefaultDatabase();
    }

    @Test
    void selectInOptimisticLockingPreTxIsResetBeforeTheConnectionIsReused() {
        ds.withTransaction(pre -> {
            pre.select(1);
            return "input";
        }, (input, tx) -> tx.value(String.class).set(key, "db1"), key);
        assertNextBorrowIsOnDefaultDatabase();
    }

    /**
     * The block wrote {@code key} on database 1. Whoever borrows the connection next must be back on
     * database 0 and not see it, whether through {@code withConnection} or a blocking command on the
     * shared data source, which borrows from the same pool.
     */
    private void assertNextBorrowIsOnDefaultDatabase() {
        assertThat(rawGetOnDatabase(1, key)).isEqualTo("db1");
        ds.withConnection(rds -> {
            assertThat(rds.execute("CLIENT", "INFO").toString()).contains(" db=0 ");
            assertThat(rds.value(String.class).get(key)).isNull();
        });
        // On database 1 this would fail with WRONGTYPE, since key holds a string there.
        assertThat(ds.list(String.class, String.class).blpop(Duration.ofMillis(100), key)).isNull();
    }

    @Test
    void rejectsNullConsumer() {
        assertThatThrownBy(() -> ds.withConnection(null)).isInstanceOf(NullPointerException.class);
    }

}

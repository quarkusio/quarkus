package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.smallrye.mutiny.Uni;

class LettuceWithTransactionBlockingIntegrationTest extends CommandsTestBase {

    RedisDataSource ds;

    @BeforeEach
    void initialize() {
        ds = blockingDataSource();
    }

    @Test
    void execHappyPathReturnsOneEntryPerQueuedCommand() {
        TransactionResult result = ds.withTransaction(tx -> {
            var value = tx.value(String.class, String.class);
            value.set("k1", "v1");
            value.get("k1");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((String) result.get(1)).isEqualTo("v1");
        assertThat(rawGet("k1")).isEqualTo("v1");
    }

    @Test
    void keyCommandsInTransactionYieldTypedResults() {
        rawSet("k1", "v1");
        TransactionResult result = ds.withTransaction(tx -> {
            var keys = tx.key(String.class);
            keys.exists("k1");
            keys.expire("k1", 100);
            keys.ttl("k1");
            keys.type("k1");
            keys.rename("k1", "k2");
            keys.del("k2");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isEqualTo(6);
        Boolean exists = result.get(0);
        Boolean expired = result.get(1);
        Long ttl = result.get(2);
        RedisValueType type = result.get(3);
        Object renamed = result.get(4);
        Integer deleted = result.get(5);
        assertThat(exists).isTrue();
        assertThat(expired).isTrue();
        assertThat(ttl).isGreaterThan(0L);
        assertThat(type).isEqualTo(RedisValueType.STRING);
        assertThat(renamed).isNull();
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void additionalMapperShapesYieldTypedResults() {
        rawSet("k1", "v1");
        TransactionResult result = ds.withTransaction(tx -> {
            var value = tx.value(String.class, String.class);
            var keys = tx.key(String.class);
            value.setAndChanged("sc", "v");
            value.mget("k1", "missing");
            value.getrange("k1", 0, 1);
            keys.dump("k1");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isEqualTo(4);
        Boolean changed = result.get(0);
        Map<String, String> values = result.get(1);
        String range = result.get(2);
        String dump = result.get(3);
        assertThat(changed).isTrue();
        assertThat(values).hasSize(2);
        assertThat(values.get("k1")).isEqualTo("v1");
        assertThat(values.get("missing")).isNull();
        assertThat(range).isEqualTo("v1");
        assertThat(dump).isNotNull();
    }

    @Test
    void lcsInTransaction() {
        rawSet("k1", "ohmytext");
        rawSet("k2", "mynewtext");
        TransactionResult result = ds.withTransaction(tx -> {
            var value = tx.value(String.class, String.class);
            value.lcs("k1", "k2");
            value.lcsLength("k1", "k2");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        String match = result.get(0);
        Long length = result.get(1);
        assertThat(match).isEqualTo("mytext");
        assertThat(length).isEqualTo(6L);
    }

    @Test
    void userBlockExceptionIssuesDiscardAndPropagates() {
        assertThatThrownBy(() -> ds.withTransaction(tx -> {
            tx.value(String.class, String.class).set("k", "v");
            throw new RuntimeException("boom");
        })).hasMessageContaining("boom");
        LettuceConnectionPool pool = ((LettuceReactiveRedisDataSourceImpl) ds.getReactive()).getPool();
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == pool.getObjectCount());
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void releaseFailureDoesNotHideTheTransactionBlocksOwnException() {
        LettuceReactiveRedisDataSourceImpl reactive = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
                pool(), pubSubConnector()) {
            @Override
            Uni<Void> releaseConnection(LettuceConnection conn) {
                return Uni.createFrom().failure(new IllegalStateException("release failed"));
            }
        };
        RedisDataSource failingRelease = new LettuceBlockingRedisDataSourceImpl(reactive, TIMEOUT);
        assertThatThrownBy(() -> failingRelease.withTransaction(tx -> {
            tx.value(String.class, String.class).set("k", "v");
            throw new RuntimeException("boom");
        }))
                .hasMessage("boom")
                .satisfies(t -> {
                    assertThat(t.getSuppressed()).hasSize(1);
                    assertThat(t.getSuppressed()[0]).isInstanceOf(IllegalStateException.class).hasMessage("release failed");
                });
        // DISCARD still ran before the failed release.
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void connectionReturnedAfterDiscardServesSubsequentWithConnection() {
        ds.withTransaction(tx -> {
            tx.value(String.class, String.class).set("k", "v");
            tx.discard();
        });
        // A connection stuck mid-transaction (DISCARD never issued) would hang this call.
        ds.withConnection(rds -> rds.execute("CLIENT", "ID"));
        LettuceConnectionPool pool = ((LettuceReactiveRedisDataSourceImpl) ds.getReactive()).getPool();
        assertThat(pool.getIdle()).isEqualTo(pool.getObjectCount());
    }

    @Test
    void explicitDiscardYieldsAbortedResult() {
        TransactionResult result = ds.withTransaction(tx -> {
            tx.value(String.class, String.class).set("k", "v");
            tx.discard();
        });
        assertThat(result.discarded()).isTrue();
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void commandIssuedAfterDiscardIsRejectedAndRecordsNothing() {
        TransactionResult result = ds.withTransaction(tx -> {
            tx.discard();
            // DISCARD already left MULTI: the holder must refuse the command instead of running it for real.
            assertThatThrownBy(() -> tx.value(String.class, String.class).set("k", "v"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to add command to the current transaction");
            assertThat(tx.discarded()).isTrue();
        });
        assertThat(result.discarded()).isTrue();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isZero();
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void watchViolationYieldsAbortedResult() {
        rawSet("watched", "initial");
        TransactionResult result = ds.withTransaction(tx -> {
            // mutate the watched key from the shared connection before EXEC
            rawSet("watched", "changed");
            tx.value(String.class, String.class).set("k", "v");
        }, "watched");
        assertThat(result.discarded()).isTrue();
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void optimisticLockingPreTxRunsOnSameConnection() {
        rawSet("counter", "10");
        OptimisticLockingTransactionResult<String> result = ds.withTransaction(
                preTx -> preTx.value(String.class, String.class).get("counter"),
                (current, tx) -> tx.value(String.class, String.class).set("counter", current + "0"),
                "counter");
        assertThat(result.discarded()).isFalse();
        assertThat(result.getPreTransactionResult()).isEqualTo("10");
        assertThat(rawGet("counter")).isEqualTo("100");
    }

    @Test
    void rejectsNullAndEmptyWatchedKeys() {
        assertThatThrownBy(() -> ds.withTransaction(tx -> {
        }, (String[]) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ds.withTransaction(tx -> {
        }, new String[0])).isInstanceOf(IllegalArgumentException.class);
    }
}

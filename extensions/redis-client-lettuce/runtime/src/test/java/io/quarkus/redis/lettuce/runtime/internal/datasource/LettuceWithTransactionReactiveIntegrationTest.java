package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.smallrye.mutiny.Uni;

class LettuceWithTransactionReactiveIntegrationTest extends CommandsTestBase {

    LettuceReactiveRedisDataSourceImpl ds;

    @BeforeEach
    void initialize() {
        ds = reactiveDataSource();
    }

    @Test
    void execHappyPathReturnsOneEntryPerQueuedCommand() {
        TransactionResult result = ds.withTransaction(tx -> {
            var value = tx.value(String.class, String.class);
            return value.set("k1", "v1").chain(() -> value.get("k1"));
        }).await().atMost(TIMEOUT);
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
            return keys.exists("k1")
                    .chain(() -> keys.expire("k1", 100))
                    .chain(() -> keys.ttl("k1"))
                    .chain(() -> keys.type("k1"))
                    .chain(() -> keys.rename("k1", "k2"))
                    .chain(() -> keys.del("k2"));
        }).await().atMost(TIMEOUT);
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
            return value.setAndChanged("sc", "v")
                    .chain(() -> value.mget("k1", "missing"))
                    .chain(() -> value.getrange("k1", 0, 1))
                    .chain(() -> keys.dump("k1"));
        }).await().atMost(TIMEOUT);
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
            return value.lcs("k1", "k2").chain(() -> value.lcsLength("k1", "k2"));
        }).await().atMost(TIMEOUT);
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
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class, String.class).set("k", "v")
                .chain(() -> Uni.createFrom().failure(new RuntimeException("boom")))).await().atMost(TIMEOUT))
                .hasMessageContaining("boom");
        await().atMost(TIMEOUT).until(() -> ds.getPool().getIdle() == ds.getPool().getObjectCount());
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void connectionReturnedAfterDiscardServesSubsequentWithConnection() {
        ds.withTransaction(tx -> tx.value(String.class, String.class).set("k", "v").chain(tx::discard))
                .await().atMost(TIMEOUT);
        // A connection stuck mid-transaction (DISCARD never issued) would hang this call.
        ds.withConnection(rds -> rds.execute("CLIENT", "ID").replaceWithVoid()).await().atMost(TIMEOUT);
        assertThat(ds.getPool().getIdle()).isEqualTo(ds.getPool().getObjectCount());
    }

    @Test
    void connectionReturnedAfterExecServesSubsequentWithConnection() {
        ds.withTransaction(tx -> tx.value(String.class, String.class).set("k", "v")).await().atMost(TIMEOUT);
        ds.withConnection(rds -> rds.execute("CLIENT", "ID").replaceWithVoid()).await().atMost(TIMEOUT);
        assertThat(ds.getPool().getIdle()).isEqualTo(ds.getPool().getObjectCount());
    }

    @Test
    void explicitDiscardYieldsAbortedResult() {
        TransactionResult result = ds
                .withTransaction(tx -> tx.value(String.class, String.class).set("k", "v").chain(tx::discard)).await()
                .atMost(TIMEOUT);
        assertThat(result.discarded()).isTrue();
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void commandIssuedAfterDiscardIsRejectedAndRecordsNothing() {
        AtomicReference<Throwable> rejected = new AtomicReference<>();
        AtomicReference<ReactiveTransactionalRedisDataSource> captured = new AtomicReference<>();
        TransactionResult result = ds.withTransaction(tx -> {
            captured.set(tx);
            var value = tx.value(String.class, String.class);
            // DISCARD already left MULTI: the holder must refuse the command instead of running it for real.
            return tx.discard()
                    .chain(() -> value.set("k", "v"))
                    .onFailure().invoke(rejected::set)
                    .onFailure().recoverWithNull()
                    .replaceWithVoid();
        }).await().atMost(TIMEOUT);
        assertThat(rejected.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to add command to the current transaction");
        assertThat(captured.get().discarded()).isTrue();
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
            return tx.value(String.class, String.class).set("k", "v");
        }, "watched").await().atMost(TIMEOUT);
        assertThat(result.discarded()).isTrue();
        assertThat(rawGet("k")).isNull();
    }

    @Test
    void optimisticLockingPreTxRunsOnSameConnection() {
        rawSet("counter", "10");
        OptimisticLockingTransactionResult<String> result = ds.withTransaction(
                preTx -> preTx.value(String.class, String.class).get("counter"),
                (current, tx) -> tx.value(String.class, String.class).set("counter", current + "0"),
                "counter").await().atMost(TIMEOUT);
        assertThat(result.discarded()).isFalse();
        assertThat(result.getPreTransactionResult()).isEqualTo("10");
        assertThat(rawGet("counter")).isEqualTo("100");
    }

    @Test
    void rejectsNullAndEmptyWatchedKeys() {
        assertThatThrownBy(() -> ds.withTransaction(tx -> Uni.createFrom().voidItem(), (String[]) null)
                .await().atMost(TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ds.withTransaction(tx -> Uni.createFrom().voidItem(), new String[0])
                .await().atMost(TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
    }
}

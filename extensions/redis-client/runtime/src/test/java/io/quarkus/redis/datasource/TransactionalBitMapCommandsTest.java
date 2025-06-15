package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.bitmap.TransactionalBitMapCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

public class TransactionalBitMapCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    public static final String KEY = "tx-bitmap-key";

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void bitMapBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalBitMapCommands<String> bitmap = tx.bitmap(String.class);
            assertThat(bitmap.getDataSource()).isEqualTo(tx);
            bitmap.bitcount(key); // 0 -> 0
            bitmap.setbit(key, 0L, 1); // 1 -> 1
            bitmap.setbit(key, 1L, 1); // 2 -> 2
            bitmap.setbit(key, 2L, 1); // 3 -> 3
            bitmap.bitcount(key, 0, -1); // 4 -> 3
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(0);
        assertThat((int) result.get(1)).isEqualTo(0);
        assertThat((int) result.get(2)).isEqualTo(0);
        assertThat((int) result.get(3)).isEqualTo(0);
        assertThat((long) result.get(4)).isEqualTo(3);
    }

    @Test
    public void bitMapBlockingDiscard() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalBitMapCommands<String> bitmap = tx.bitmap(String.class);
            bitmap.bitcount(key); // 0 -> 0
            bitmap.setbit(key, 0L, 1); // 1 -> 1
            bitmap.setbit(key, 1L, 1); // 2 -> 2
            bitmap.setbit(key, 2L, 1); // 3 -> 3
            tx.discard();
            assertThatThrownBy(() -> bitmap.bitcount(key, 0, -1)).isInstanceOf(IllegalStateException.class);
        });
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

    @Test
    public void bitMapReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalBitMapCommands<String> bitmap = tx.bitmap(String.class);
            return bitmap.bitcount(key).chain(() -> bitmap.setbit(key, 0L, 1)).chain(() -> bitmap.setbit(key, 1L, 1))
                    .chain(() -> bitmap.setbit(key, 2L, 1)).chain(() -> bitmap.bitcount(key, 0, -1));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(0);
        assertThat((int) result.get(1)).isEqualTo(0);
        assertThat((int) result.get(2)).isEqualTo(0);
        assertThat((int) result.get(3)).isEqualTo(0);
        assertThat((long) result.get(4)).isEqualTo(3);
    }

    @Test
    public void bitMapReactiveDiscard() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalBitMapCommands<String> bitmap = tx.bitmap(String.class);
            return bitmap.bitcount(key).chain(() -> bitmap.setbit(key, 0L, 1)).chain(() -> bitmap.setbit(key, 1L, 1))
                    .chain(() -> bitmap.setbit(key, 2L, 1)).chain(tx::discard);
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

}

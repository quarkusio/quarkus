package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresRedis6OrHigher
public class TransactionalHashCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    public static final String KEY = "tx-hash-key";

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
    public void hgetBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            assertThat(hash.getDataSource()).isEqualTo(tx);
            hash.hget(KEY, "field"); // 0 -> null
            hash.hset(KEY, "field", "hello"); // 1 -> true
            hash.hget(KEY, "field"); // 2 -> "hello
            hash.hdel(KEY, "field", "field2"); // 3 -> 1
            hash.hget(KEY, "field"); // 4 -> null
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    public void hgetBlockingWithWatch() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            hash.hget(KEY, "field"); // 0 -> null
            hash.hset(KEY, "field", "hello"); // 1 -> true
            hash.hget(KEY, "field"); // 2 -> "hello
            hash.hdel(KEY, "field", "field2"); // 3 -> 1
            hash.hget(KEY, "field"); // 4 -> null
        }, KEY);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    public void hgetBlockingWithWatchAndDiscard() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            hash.hget(KEY, "field"); // 0 -> null
            hash.hset(KEY, "field", "hello"); // 1 -> true
            hash.hget(KEY, "field"); // 2 -> "hello

            // Update the key - that will discard the transaction
            blocking.hash(String.class).hset(KEY, "toto", "updated");

            hash.hdel(KEY, "field", "field2"); // 3 -> 1
            hash.hget(KEY, "field"); // 4 -> null
        }, KEY);
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

    @Test
    public void hgetReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(KEY, "field") // 0 -> null
                    .chain(() -> hash.hset(KEY, "field", "hello")) // 1 -> true
                    .chain(() -> hash.hget(KEY, "field")) // 2 -> "hello
                    .chain(() -> hash.hdel(KEY, "field", "field2")) // 3 -> 1
                    .chain(() -> hash.hget(KEY, "field")); // 4 -> null
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    public void hgetReactiveWithWatch() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(KEY, "field") // 0 -> null
                    .chain(() -> hash.hset(KEY, "field", "hello")) // 1 -> true
                    .chain(() -> hash.hget(KEY, "field")) // 2 -> "hello
                    .chain(() -> hash.hdel(KEY, "field", "field2")) // 3 -> 1
                    .chain(() -> hash.hget(KEY, "field")); // 4 -> null
        }, KEY).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    public void hgetReactiveWithWatchAndDiscard() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(KEY, "field").chain(() -> hash.hset(KEY, "field", "hello"))
                    .chain(() -> hash.hget(KEY, "field")).chain(() -> reactive.hash(String.class).hset(KEY, "a", "b"))
                    .chain(() -> hash.hdel(KEY, "field", "field2")).chain(() -> hash.hget(KEY, "field"));
        }, KEY).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

}

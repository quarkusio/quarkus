package io.quarkus.redis.lettuce.runtime.internal.hash;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalHashCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void hgetBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            assertThat(hash.getDataSource()).isEqualTo(tx);
            hash.hget(key, "field"); // 0 -> null
            hash.hset(key, "field", "hello"); // 1 -> true
            hash.hget(key, "field"); // 2 -> "hello
            hash.hdel(key, "field", "field2"); // 3 -> 1
            hash.hget(key, "field"); // 4 -> null
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
    void hgetBlockingWithWatch() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            hash.hget(key, "field"); // 0 -> null
            hash.hset(key, "field", "hello"); // 1 -> true
            hash.hget(key, "field"); // 2 -> "hello
            hash.hdel(key, "field", "field2"); // 3 -> 1
            hash.hget(key, "field"); // 4 -> null
        }, key);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    void hgetBlockingWithWatchAndDiscard() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            hash.hget(key, "field"); // 0 -> null
            hash.hset(key, "field", "hello"); // 1 -> true
            hash.hget(key, "field"); // 2 -> "hello

            // Update the key - that will discard the transaction
            blockingDs.hash(String.class).hset(key, "toto", "updated");

            hash.hdel(key, "field", "field2"); // 3 -> 1
            hash.hget(key, "field"); // 4 -> null
        }, key);
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

    @Test
    void hgetReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(key, "field") // 0 -> null
                    .chain(() -> hash.hset(key, "field", "hello")) // 1 -> true
                    .chain(() -> hash.hget(key, "field")) // 2 -> "hello
                    .chain(() -> hash.hdel(key, "field", "field2")) // 3 -> 1
                    .chain(() -> hash.hget(key, "field")); // 4 -> null
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
    void hgetReactiveWithWatch() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(key, "field") // 0 -> null
                    .chain(() -> hash.hset(key, "field", "hello")) // 1 -> true
                    .chain(() -> hash.hget(key, "field")) // 2 -> "hello
                    .chain(() -> hash.hdel(key, "field", "field2")) // 3 -> 1
                    .chain(() -> hash.hget(key, "field")); // 4 -> null
        }, key).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((String) result.get(2)).isEqualTo("hello");
        assertThat((int) result.get(3)).isEqualTo(1);
        assertThat((Void) result.get(4)).isNull();
    }

    @Test
    void hgetReactiveWithWatchAndDiscard() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hget(key, "field")
                    .chain(() -> hash.hset(key, "field", "hello"))
                    .chain(() -> hash.hget(key, "field"))
                    .chain(() -> reactiveDs.hash(String.class).hset(key, "a", "b"))
                    .chain(() -> hash.hdel(key, "field", "field2"))
                    .chain(() -> hash.hget(key, "field"));
        }, key).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.discarded()).isTrue();
    }

}

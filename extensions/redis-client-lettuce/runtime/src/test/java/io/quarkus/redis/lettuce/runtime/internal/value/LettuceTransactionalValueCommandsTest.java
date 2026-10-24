package io.quarkus.redis.lettuce.runtime.internal.value;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.string.TransactionalStringCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.quarkus.redis.datasource.value.TransactionalValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalValueCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void valueBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalValueCommands<String, String> string = tx.value(String.class);
            assertThat(string.getDataSource()).isEqualTo(tx);
            string.set(key, "hello");
            string.setnx("k2", "bonjour");
            string.append(key, "-1");
            string.get(key);
            string.strlen("k2");
            string.get("nonexisting_key");
        });
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
        assertThat((Object) result.get(5)).isNull();
    }

    @Test
    void valueReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalValueCommands<String, String> string = tx.value(String.class);
            return string.set(key, "hello")
                    .chain(() -> string.setnx("k2", "bonjour"))
                    .chain(() -> string.append(key, "-1"))
                    .chain(() -> string.get(key))
                    .chain(() -> string.strlen("k2"))
                    .chain(() -> string.get("nonexisting_key"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
        assertThat((Object) result.get(5)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    void stringBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalStringCommands<String, String> string = tx.string(String.class, String.class);
            assertThat(string.getDataSource()).isEqualTo(tx);
            string.set(key, "hello");
            string.setnx("k2", "bonjour");
            string.append(key, "-1");
            string.get(key);
            string.strlen("k2");
            string.setGet(key, "world", new SetArgs().ex(10));
            string.get("nonexisting_key");
        });
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
        assertThat((String) result.get(5)).isEqualTo("hello-1");
        assertThat((Object) result.get(6)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    void stringReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalStringCommands<String, String> string = tx.string(String.class, String.class);
            assertThat(string.getDataSource()).isEqualTo(tx);
            return string.set(key, "hello")
                    .chain(() -> string.setnx("k2", "bonjour"))
                    .chain(() -> string.append(key, "-1"))
                    .chain(() -> string.get(key))
                    .chain(() -> string.strlen("k2"))
                    .chain(() -> string.setGet(key, "world", new SetArgs().ex(10)))
                    .chain(() -> string.get("nonexisting_key"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
        assertThat((String) result.get(5)).isEqualTo("hello-1");
        assertThat((Object) result.get(6)).isNull();
    }

}

package io.quarkus.redis.lettuce.runtime.internal.key;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.datasource.keys.TransactionalKeyCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalKeyCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void keyBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalKeyCommands<String> keys = tx.key();
            assertThat(keys.getDataSource()).isEqualTo(tx);
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            assertThat(hash.getDataSource()).isEqualTo(tx);
            hash.hset("k1", Map.of("1", "a", "2", "b", "3", "c"));
            hash.hset("k2", "4", "d");
            keys.type("k1");
            keys.expire("k2", Duration.ofSeconds(10));
            keys.del("k1");
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((RedisValueType) result.get(2)).isEqualTo(RedisValueType.HASH);
        assertThat((boolean) result.get(3)).isTrue();
        assertThat((int) result.get(4)).isEqualTo(1);
    }

    @Test
    void keyReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalKeyCommands<String> keys = tx.key(String.class);
            ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
            return hash.hset("k1", Map.of("1", "a", "2", "b", "3", "c"))
                    .chain(() -> hash.hset("k2", "4", "d"))
                    .chain(() -> keys.type("k1"))
                    .chain(() -> keys.expire("k2", Duration.ofSeconds(10)))
                    .chain(() -> keys.del("k1"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((RedisValueType) result.get(2)).isEqualTo(RedisValueType.HASH);
        assertThat((boolean) result.get(3)).isTrue();
        assertThat((int) result.get(4)).isEqualTo(1);
    }

}

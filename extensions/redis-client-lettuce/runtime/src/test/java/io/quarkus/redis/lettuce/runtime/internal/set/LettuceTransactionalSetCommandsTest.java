package io.quarkus.redis.lettuce.runtime.internal.set;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.set.TransactionalSetCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalSetCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void setBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalSetCommands<String, String> set = tx.set(String.class);
            assertThat(set.getDataSource()).isEqualTo(tx);
            set.sadd(key, "a", "b", "c", "d");
            set.sadd(key, "c", "1");
            set.sismember(key, "1");
            set.spop(key);
            set.scard(key);
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat((int) result.get(1)).isEqualTo(1);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((String) result.get(3)).isNotBlank();
        assertThat((long) result.get(4)).isEqualTo(4);
    }

    @Test
    void setReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalSetCommands<String, String> set = tx.set(String.class);
            return set.sadd(key, "a", "b", "c", "d")
                    .chain(() -> set.sadd(key, "c", "1"))
                    .chain(() -> set.sismember(key, "1"))
                    .chain(() -> set.spop(key))
                    .chain(() -> set.scard(key));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat((int) result.get(1)).isEqualTo(1);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((String) result.get(3)).isNotBlank();
        assertThat((long) result.get(4)).isEqualTo(4);
    }

}

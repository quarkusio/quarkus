package io.quarkus.redis.lettuce.runtime.internal.list;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.list.TransactionalListCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalListCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void listBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalListCommands<String, String> list = tx.list(String.class);
            assertThat(list.getDataSource()).isEqualTo(tx);
            list.lpush(key, "a", "b", "c", "d");
            list.linsertBeforePivot(key, "c", "1");
            list.lpos(key, "c");
            list.llen(key);
            list.lpop(key);
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(4);
        assertThat((long) result.get(1)).isEqualTo(5);
        assertThat((long) result.get(2)).isEqualTo(2);
        assertThat((long) result.get(3)).isEqualTo(5);
        assertThat((String) result.get(4)).isEqualTo("d");
    }

    @Test
    void listReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalListCommands<String, String> list = tx.list(String.class);
            return list.lpush(key, "a", "b", "c", "d")
                    .chain(() -> list.linsertBeforePivot(key, "c", "1"))
                    .chain(() -> list.lpos(key, "c"))
                    .chain(() -> list.llen(key))
                    .chain(() -> list.lpop(key));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(4);
        assertThat((long) result.get(1)).isEqualTo(5);
        assertThat((long) result.get(2)).isEqualTo(2);
        assertThat((long) result.get(3)).isEqualTo(5);
        assertThat((String) result.get(4)).isEqualTo("d");
    }

}

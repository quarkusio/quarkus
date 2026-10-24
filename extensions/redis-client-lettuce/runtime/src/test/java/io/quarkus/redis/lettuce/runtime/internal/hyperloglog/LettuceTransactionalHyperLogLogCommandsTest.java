package io.quarkus.redis.lettuce.runtime.internal.hyperloglog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.TransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalHyperLogLogCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void hllBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalHyperLogLogCommands<String, String> hll = tx.hyperloglog(String.class);
            assertThat(hll.getDataSource()).isEqualTo(tx);
            hll.pfadd(key, "a", "b", "c", "d"); // 0 -> true
            hll.pfcount(key); // 1 -> 4
            hll.pfadd(key, "a", "d", "e"); // 2 -> true
            hll.pfcount(key); // 3 -> 5
            hll.pfmerge(key + "-merged", key); // 4 -> null (discarded)
            hll.pfcount(key + "-merged"); // 5 -> 5
        });
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat((boolean) result.get(0)).isTrue();
        assertThat((long) result.get(1)).isEqualTo(4);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((long) result.get(3)).isEqualTo(5);
        assertThat((Object) result.get(4)).isNull();
        assertThat((long) result.get(5)).isEqualTo(5);
    }

    @Test
    void hllReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalHyperLogLogCommands<String, String> hll = tx.hyperloglog(String.class);
            return hll.pfadd(key, "a", "b", "c", "d")
                    .chain(() -> hll.pfcount(key))
                    .chain(() -> hll.pfadd(key, "a", "d", "e"))
                    .chain(() -> hll.pfcount(key))
                    .chain(() -> hll.pfmerge(key + "-merged", key))
                    .chain(() -> hll.pfcount(key + "-merged"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat((boolean) result.get(0)).isTrue();
        assertThat((long) result.get(1)).isEqualTo(4);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((long) result.get(3)).isEqualTo(5);
        assertThat((Object) result.get(4)).isNull();
        assertThat((long) result.get(5)).isEqualTo(5);
    }

}

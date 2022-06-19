package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.api.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.api.hyperloglog.TransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.quarkus.redis.datasource.impl.BlockingRedisDataSourceImpl;
import io.quarkus.redis.datasource.impl.ReactiveRedisDataSourceImpl;

public class TransactionalHyperLogLogCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void hllBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalHyperLogLogCommands<String, String> hll = tx.hyperloglog(String.class);
            hll.pfadd(key, "a", "b", "c", "d"); // 0 -> true
            hll.pfcount(key); // 1 -> 4
            hll.pfadd(key, "a", "d", "e"); // 2 -> true
            hll.pfcount(key); // 4 -> 5
        });
        assertThat(result.size()).isEqualTo(4);
        assertThat(result.discarded()).isFalse();
        assertThat((boolean) result.get(0)).isTrue();
        assertThat((long) result.get(1)).isEqualTo(4);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((long) result.get(3)).isEqualTo(5);
    }

    @Test
    public void hllReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalHyperLogLogCommands<String, String> hll = tx.hyperloglog(String.class);
            return hll.pfadd(key, "a", "b", "c", "d")
                    .chain(() -> hll.pfcount(key))
                    .chain(() -> hll.pfadd(key, "a", "d", "e"))
                    .chain(() -> hll.pfcount(key));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(4);
        assertThat(result.discarded()).isFalse();
        assertThat((boolean) result.get(0)).isTrue();
        assertThat((long) result.get(1)).isEqualTo(4);
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((long) result.get(3)).isEqualTo(5);
    }

}

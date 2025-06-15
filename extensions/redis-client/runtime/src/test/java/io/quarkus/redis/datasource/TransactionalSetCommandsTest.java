package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.set.TransactionalSetCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresRedis6OrHigher
public class TransactionalSetCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

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
    public void setBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalSetCommands<String, String> set = tx.set(String.class);
            assertThat(set.getDataSource()).isEqualTo(tx);
            set.sadd(key, "a", "b", "c", "d");
            set.sadd(key, "c", "1");
            set.spop(key);
            set.scard(key);
            set.sismember(key, "1");
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat((int) result.get(1)).isEqualTo(1);
        assertThat((String) result.get(2)).isNotBlank();
        assertThat((long) result.get(3)).isEqualTo(4);
    }

    @Test
    public void setReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalSetCommands<String, String> set = tx.set(String.class);
            return set.sadd(key, "a", "b", "c", "d").chain(() -> set.sadd(key, "c", "1")).chain(() -> set.spop(key))
                    .chain(() -> set.scard(key)).chain(() -> set.sismember(key, "1"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat((int) result.get(1)).isEqualTo(1);
        assertThat((String) result.get(2)).isNotBlank();
        assertThat((long) result.get(3)).isEqualTo(4);
    }

}

package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.api.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.api.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.api.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.api.keys.RedisValueType;
import io.quarkus.redis.datasource.api.keys.TransactionalKeyCommands;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.quarkus.redis.datasource.impl.BlockingRedisDataSourceImpl;
import io.quarkus.redis.datasource.impl.ReactiveRedisDataSourceImpl;

public class TransactionalKeyTest extends DatasourceTestBase {

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
    public void keyBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalKeyCommands<String> keys = tx.key();
            TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
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
    public void keyReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
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

package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.TransactionalSortedSetCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresRedis6OrHigher
public class TransactionalSortedSetCommandsTest extends DatasourceTestBase {

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
            TransactionalSortedSetCommands<String, String> set = tx.sortedSet(String.class);
            assertThat(set.getDataSource()).isEqualTo(tx);
            set.zadd(key, Map.of("a", 1.0, "b", 2.0, "c", 3.0, "d", 4.0));
            set.zadd(key, 3.0, "e");
            set.zpopmin(key);
            set.zcard(key);
            set.zpopmax(key, 3);
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat(result.<Boolean> get(1)).isTrue();
        assertThat(result.<ScoredValue<String>> get(2)).isNotNull()
                .satisfies(s -> assertThat(s.value()).isEqualTo("a"));
        assertThat((long) result.get(3)).isEqualTo(4);
        assertThat(result.<List<ScoredValue<String>>> get(4)).hasSize(3);
    }

    @Test
    public void setReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalSortedSetCommands<String, String> set = tx.sortedSet(String.class);
            return set.zadd(key, Map.of("a", 1.0, "b", 2.0, "c", 3.0, "d", 4.0)).chain(() -> set.zadd(key, 3.0, "e"))
                    .chain(() -> set.zpopmin(key)).chain(() -> set.zcard(key)).chain(() -> set.zpopmax(key, 3));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((int) result.get(0)).isEqualTo(4);
        assertThat(result.<Boolean> get(1)).isTrue();
        assertThat(result.<ScoredValue<String>> get(2)).isNotNull()
                .satisfies(s -> assertThat(s.value()).isEqualTo("a"));
        assertThat((long) result.get(3)).isEqualTo(4);
        assertThat(result.<List<ScoredValue<String>>> get(4)).hasSize(3);
    }

}

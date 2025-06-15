package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.list.TransactionalListCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresRedis6OrHigher
public class TransactionalListCommandsTest extends DatasourceTestBase {

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
    public void listBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
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
    public void listReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalListCommands<String, String> list = tx.list(String.class);
            return list.lpush(key, "a", "b", "c", "d").chain(() -> list.linsertBeforePivot(key, "c", "1"))
                    .chain(() -> list.lpos(key, "c")).chain(() -> list.llen(key)).chain(() -> list.lpop(key));
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

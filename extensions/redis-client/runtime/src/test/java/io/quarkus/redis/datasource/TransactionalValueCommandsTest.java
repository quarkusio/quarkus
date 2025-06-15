
package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.quarkus.redis.datasource.value.TransactionalValueCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresRedis6OrHigher
public class TransactionalValueCommandsTest extends DatasourceTestBase {

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
            TransactionalValueCommands<String, String> string = tx.value(String.class);
            assertThat(string.getDataSource()).isEqualTo(tx);
            string.set(key, "hello");
            string.setnx("k2", "bonjour");
            string.append(key, "-1");
            string.get(key);
            string.strlen("k2");
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
    }

    @Test
    public void setReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalValueCommands<String, String> string = tx.value(String.class);
            return string.set(key, "hello").chain(() -> string.setnx("k2", "bonjour"))
                    .chain(() -> string.append(key, "-1")).chain(() -> string.get(key))
                    .chain(() -> string.strlen("k2"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat(result.<Void> get(0)).isNull();
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((long) result.get(2)).isEqualTo(7L);
        assertThat((String) result.get(3)).isEqualTo("hello-1");
        assertThat((long) result.get(4)).isEqualTo(7L);
    }

}

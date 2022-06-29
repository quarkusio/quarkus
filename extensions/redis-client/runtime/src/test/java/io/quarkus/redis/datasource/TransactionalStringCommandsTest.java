
package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.api.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.api.string.TransactionalStringCommands;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.quarkus.redis.datasource.impl.BlockingRedisDataSourceImpl;
import io.quarkus.redis.datasource.impl.ReactiveRedisDataSourceImpl;

public class TransactionalStringCommandsTest extends DatasourceTestBase {

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
    public void setBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalStringCommands<String, String> string = tx.string(String.class);
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
            ReactiveTransactionalStringCommands<String, String> string = tx.string(String.class);
            return string.set(key, "hello")
                    .chain(() -> string.setnx("k2", "bonjour"))
                    .chain(() -> string.append(key, "-1"))
                    .chain(() -> string.get(key))
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

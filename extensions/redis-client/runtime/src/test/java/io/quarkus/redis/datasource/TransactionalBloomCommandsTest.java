package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.bloom.TransactionalBloomCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings("unchecked")
@RequiresCommand("bf.add")
public class TransactionalBloomCommandsTest extends DatasourceTestBase {

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
    public void bloomBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalBloomCommands<String, String> bloom = tx.bloom(String.class);
            assertThat(bloom.getDataSource()).isEqualTo(tx);
            bloom.bfmadd(key, "a", "b", "c", "d", "a"); // 0 -> 4 true, 1 false
            bloom.bfadd(key, "x"); // 1 -> true
            bloom.bfexists(key, "a"); // 2 -> true
            bloom.bfmexists(key, "a", "b", "z"); // 3 -> true, true, false
            bloom.bfinsert(key, new BfInsertArgs(), "v", "w", "b"); // 4 -> true, true, false
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Boolean>) result.get(0)).containsExactly(true, true, true, true, false);
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((List<Boolean>) result.get(3)).containsExactly(true, true, false);
        assertThat((List<Boolean>) result.get(4)).containsExactly(true, true, false);
    }

    @Test
    public void bloomReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalBloomCommands<String, String> bloom = tx.bloom(String.class);
            assertThat(bloom.getDataSource()).isEqualTo(tx);
            return bloom.bfmadd(key, "a", "b", "c", "d", "a") // 0 -> 4 true, 1 false
                    .chain(() -> bloom.bfadd(key, "x")) // 1 -> true
                    .chain(() -> bloom.bfexists(key, "a")) // 2 -> true
                    .chain(() -> bloom.bfmexists(key, "a", "b", "z")) // 3 -> true, true, false
                    .chain(() -> bloom.bfinsert(key, new BfInsertArgs(), "v", "w", "b")); // 4 -> true, true, false
        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Boolean>) result.get(0)).containsExactly(true, true, true, true, false);
        assertThat((boolean) result.get(1)).isTrue();
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((List<Boolean>) result.get(3)).containsExactly(true, true, false);
        assertThat((List<Boolean>) result.get(4)).containsExactly(true, true, false);
    }

}

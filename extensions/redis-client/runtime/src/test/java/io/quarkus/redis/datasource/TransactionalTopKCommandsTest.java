package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.topk.TransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings({ "unchecked", "ConstantConditions" })
@RequiresCommand("topk.add")
public class TransactionalTopKCommandsTest extends DatasourceTestBase {

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
    public void topKBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalTopKCommands<String, String> topk = tx.topk(String.class);
            assertThat(topk.getDataSource()).isEqualTo(tx);
            topk.topkReserve(key, 3);
            topk.topkAdd(key, "a", "a", "b", "b", "a", "c", "a", "b", "c", "d", "d", "d");
            topk.topkIncrBy(key, "c", 10);
            topk.topkQuery(key, "a");
            topk.topkQuery(key, "a", "b", "c");
            topk.topkList(key);
            topk.topkListWithCount(key);
        });
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((List<String>) result.get(1)).containsExactly(null, null, null, null, null, null, null, null, null,
                null, "c", null);
        assertThat((String) result.get(2)).isEqualTo("b");
        assertThat((boolean) result.get(3)).isEqualTo(true);
        assertThat((List<Boolean>) result.get(4)).containsExactly(true, false, true);
        assertThat((List<String>) result.get(5)).containsExactly("c", "a", "d");
        assertThat((Map<String, Integer>) result.get(6)).containsExactly(entry("c", 12), entry("a", 4), entry("d", 3));
    }

    @Test
    public void topKReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalTopKCommands<String, String> topk = tx.topk(String.class);
            assertThat(topk.getDataSource()).isEqualTo(tx);
            return topk.topkReserve(key, 3)
                    .chain(() -> topk.topkAdd(key, "a", "a", "b", "b", "a", "c", "a", "b", "c", "d", "d", "d"))
                    .chain(() -> topk.topkIncrBy(key, "c", 10)).chain(() -> topk.topkQuery(key, "a"))
                    .chain(() -> topk.topkQuery(key, "a", "b", "c")).chain(() -> topk.topkList(key))
                    .chain(() -> topk.topkListWithCount(key)).replaceWithVoid();
        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((List<String>) result.get(1)).containsExactly(null, null, null, null, null, null, null, null, null,
                null, "c", null);
        assertThat((String) result.get(2)).isEqualTo("b");
        assertThat((boolean) result.get(3)).isEqualTo(true);
        assertThat((List<Boolean>) result.get(4)).containsExactly(true, false, true);
        assertThat((List<String>) result.get(5)).containsExactly("c", "a", "d");
        assertThat((Map<String, Integer>) result.get(6)).containsExactly(entry("c", 12), entry("a", 4), entry("d", 3));
    }

}

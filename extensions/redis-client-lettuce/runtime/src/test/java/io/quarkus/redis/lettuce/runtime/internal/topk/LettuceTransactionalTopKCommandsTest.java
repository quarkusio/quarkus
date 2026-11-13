package io.quarkus.redis.lettuce.runtime.internal.topk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.topk.TransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

@SuppressWarnings({ "unchecked", "ConstantConditions" })
class LettuceTransactionalTopKCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void topKBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
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
        assertCommonResult(result);
    }

    @Test
    void topKReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalTopKCommands<String, String> topk = tx.topk(String.class);
            assertThat(topk.getDataSource()).isEqualTo(tx);
            return topk.topkReserve(key, 3)
                    .chain(() -> topk.topkAdd(key, "a", "a", "b", "b", "a", "c", "a", "b", "c", "d", "d", "d"))
                    .chain(() -> topk.topkIncrBy(key, "c", 10))
                    .chain(() -> topk.topkQuery(key, "a"))
                    .chain(() -> topk.topkQuery(key, "a", "b", "c"))
                    .chain(() -> topk.topkList(key))
                    .chain(() -> topk.topkListWithCount(key))
                    .replaceWithVoid();
        }).await().indefinitely();
        assertCommonResult(result);
    }

    private static void assertCommonResult(TransactionResult result) {
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        // "c" is evicted by the third "d", then re-enters on the increment and evicts "b"
        assertThat((List<String>) result.get(1)).containsExactly(null, null, null, null, null, null, null, null, null, null,
                "c", null);
        assertThat((String) result.get(2)).isEqualTo("b");
        assertThat((boolean) result.get(3)).isEqualTo(true);
        assertThat((List<Boolean>) result.get(4)).containsExactly(true, false, true);
        assertThat((List<String>) result.get(5)).containsExactly("c", "a", "d");
        assertThat((Map<String, Integer>) result.get(6)).containsExactly(entry("c", 12), entry("a", 4), entry("d", 3));
    }

}

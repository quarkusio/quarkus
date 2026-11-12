package io.quarkus.redis.lettuce.runtime.internal.countmin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.countmin.TransactionalCountMinCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

@SuppressWarnings({ "unchecked", "ConstantConditions" })
class LettuceTransactionalCountMinCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void countMinBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalCountMinCommands<String, String> cm = tx.countmin(String.class);
            assertThat(cm.getDataSource()).isEqualTo(tx);
            cm.cmsInitByDim(key, 10, 10);
            cm.cmsIncrBy(key, Map.of("a", 5L, "b", 2L, "c", 4L)); // 1 -> [5,2,4]
            cm.cmsIncrBy(key, "a", 2); // 2 -> 7
            cm.cmsQuery(key, "a"); // 3 -> 7
            cm.cmsQuery(key, "b", "c"); // 4 -> [2, 4]
        });
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((List<Long>) result.get(1)).containsExactlyInAnyOrder(5L, 2L, 4L);
        assertThat((Long) result.get(2)).isEqualTo(7);
        assertThat((Long) result.get(3)).isEqualTo(7);
        assertThat((List<Long>) result.get(4)).containsExactly(2L, 4L);
    }

    @Test
    void countMinReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalCountMinCommands<String, String> cm = tx.countmin(String.class);
            assertThat(cm.getDataSource()).isEqualTo(tx);
            return cm.cmsInitByDim(key, 10, 10)
                    .chain(() -> cm.cmsIncrBy(key, Map.of("a", 5L, "b", 2L, "c", 4L)))
                    .chain(() -> cm.cmsIncrBy(key, "a", 2))
                    .chain(() -> cm.cmsQuery(key, "a"))
                    .chain(() -> cm.cmsQuery(key, "b", "c"))
                    .replaceWithVoid();
        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((Void) result.get(0)).isNull();
        assertThat((List<Long>) result.get(1)).containsExactlyInAnyOrder(5L, 2L, 4L);
        assertThat((Long) result.get(2)).isEqualTo(7);
        assertThat((Long) result.get(3)).isEqualTo(7);
        assertThat((List<Long>) result.get(4)).containsExactly(2L, 4L);
    }

}

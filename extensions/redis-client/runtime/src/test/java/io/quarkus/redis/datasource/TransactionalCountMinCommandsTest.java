package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.countmin.TransactionalCountMinCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings({ "unchecked", "ConstantConditions" })
@RequiresCommand("cms.query")
public class TransactionalCountMinCommandsTest extends DatasourceTestBase {

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
    public void countMinBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
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
    public void countMinReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalCountMinCommands<String, String> cm = tx.countmin(String.class);
            assertThat(cm.getDataSource()).isEqualTo(tx);
            return cm.cmsInitByDim(key, 10, 10).chain(() -> cm.cmsIncrBy(key, Map.of("a", 5L, "b", 2L, "c", 4L)))
                    .chain(() -> cm.cmsIncrBy(key, "a", 2)).chain(() -> cm.cmsQuery(key, "a"))
                    .chain(() -> cm.cmsQuery(key, "b", "c")).replaceWithVoid();
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

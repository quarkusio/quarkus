package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.cuckoo.TransactionalCuckooCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings("unchecked")
@RequiresCommand("cf.add")
public class TransactionalCuckooCommandsTest extends DatasourceTestBase {

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
    public void cuckooBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalCuckooCommands<String, String> cuckoo = tx.cuckoo(String.class);
            assertThat(cuckoo.getDataSource()).isEqualTo(tx);
            cuckoo.cfinsertnx(key, "a", "b", "c", "d", "a"); // 0 -> 4 true, 1 false
            cuckoo.cfadd(key, "x"); // 1 -> void
            cuckoo.cfexists(key, "a"); // 2 -> true
            cuckoo.cfmexists(key, "a", "b", "z"); // 3 -> true, true, false
            cuckoo.cfinsert(key, new CfInsertArgs(), "v", "w", "b"); // 4 -> void
            cuckoo.cfadd(key, "a"); // 5 -> void
            cuckoo.cfcount(key, "a"); // 6 -> 2
            cuckoo.cfdel(key, "a"); // 7 -> true
            cuckoo.cfcount(key, "a"); // 8 -> 1
        });
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Boolean>) result.get(0)).containsExactly(true, true, true, true, false);
        assertThat((Void) result.get(1)).isNull();
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((List<Boolean>) result.get(3)).containsExactly(true, true, false);
        assertThat((long) result.get(6)).isEqualTo(2L);
        assertThat((boolean) result.get(7)).isTrue();
        assertThat((long) result.get(8)).isEqualTo(1L);
    }

    @Test
    public void cuckooReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalCuckooCommands<String, String> cuckoo = tx.cuckoo(String.class);
            assertThat(cuckoo.getDataSource()).isEqualTo(tx);
            return cuckoo.cfinsertnx(key, "a", "b", "c", "d", "a") // 0 -> 4 true, 1 false
                    .chain(() -> cuckoo.cfadd(key, "x")) // 1 -> void
                    .chain(() -> cuckoo.cfexists(key, "a")) // 2 -> true
                    .chain(() -> cuckoo.cfmexists(key, "a", "b", "z")) // 3 -> true, true, false
                    .chain(() -> cuckoo.cfinsert(key, new CfInsertArgs(), "v", "w", "b")) // 4 -> void
                    .chain(() -> cuckoo.cfadd(key, "a")) // 5 -> void
                    .chain(() -> cuckoo.cfcount(key, "a")) // 6 -> 2
                    .chain(() -> cuckoo.cfdel(key, "a")) // 7 -> true
                    .chain(() -> cuckoo.cfcount(key, "a")); // 8 -> 1
        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Boolean>) result.get(0)).containsExactly(true, true, true, true, false);
        assertThat((Void) result.get(1)).isNull();
        assertThat((boolean) result.get(2)).isTrue();
        assertThat((List<Boolean>) result.get(3)).containsExactly(true, true, false);
        assertThat((long) result.get(6)).isEqualTo(2L);
        assertThat((boolean) result.get(7)).isTrue();
        assertThat((long) result.get(8)).isEqualTo(1L);
    }

}

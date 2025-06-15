package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresCommand("ft.create")
public class TransactionalAutoSuggestCommandsTest extends DatasourceTestBase {

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
    public void autoSuggestBlocking() {

        TransactionResult result = blocking.withTransaction(tx -> {
            var auto = tx.autosuggest();
            assertThat(auto.getDataSource()).isEqualTo(tx);
            auto.ftSugAdd(key, "abc", 1.0);
            auto.ftSugAdd(key, "abcd", 1.0);
            auto.ftSugAdd(key, "abcde", 2.0);

            auto.ftSugAdd(key, "boo", 20);
            auto.ftSugDel(key, "boo");
            auto.ftSugLen(key);

            auto.ftSugget(key, "abcd");
            auto.ftSugget(key, "ab", new GetArgs().max(1).withScores());
        });

        assertThat(result.size()).isEqualTo(8);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(1);
        assertThat((long) result.get(1)).isEqualTo(2);
        assertThat((long) result.get(2)).isEqualTo(3);
        assertThat((long) result.get(3)).isEqualTo(4);
        assertThat((boolean) result.get(4)).isTrue();
        assertThat((long) result.get(5)).isEqualTo(3);
        assertThat((List<?>) result.get(6)).hasSize(2);
        assertThat((List<?>) result.get(7)).hasSize(1);

        List<Suggestion> sug1 = result.get(6);
        assertThat(sug1.stream().map(Suggestion::suggestion).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("abcd", "abcde");
        List<Suggestion> sug2 = result.get(7);
        assertThat(sug2.get(0).suggestion()).isEqualTo("abcde");
        assertThat(sug2.get(0).score()).isEqualTo(1.0);
    }

    @Test
    public void autoSuggestReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            var auto = tx.autosuggest();
            assertThat(auto.getDataSource()).isEqualTo(tx);
            var u1 = auto.ftSugAdd(key, "abc", 1.0);
            var u2 = auto.ftSugAdd(key, "abcd", 1.0);
            var u3 = auto.ftSugAdd(key, "abcde", 2.0);

            var u4 = auto.ftSugAdd(key, "boo", 20);
            var u5 = auto.ftSugDel(key, "boo");
            var u6 = auto.ftSugLen(key);

            var u7 = auto.ftSugget(key, "abcd");
            var u8 = auto.ftSugget(key, "ab", new GetArgs().max(1).withScores());

            return u1.chain(() -> u2).chain(() -> u3).chain(() -> u4).chain(() -> u5).chain(() -> u6).chain(() -> u7)
                    .chain(() -> u8);
        }).await().indefinitely();

        assertThat(result.size()).isEqualTo(8);
        assertThat(result.discarded()).isFalse();
        assertThat((long) result.get(0)).isEqualTo(1);
        assertThat((long) result.get(1)).isEqualTo(2);
        assertThat((long) result.get(2)).isEqualTo(3);
        assertThat((long) result.get(3)).isEqualTo(4);
        assertThat((boolean) result.get(4)).isTrue();
        assertThat((long) result.get(5)).isEqualTo(3);
        assertThat((List<?>) result.get(6)).hasSize(2);
        assertThat((List<?>) result.get(7)).hasSize(1);

        List<Suggestion> sug1 = result.get(6);
        assertThat(sug1.stream().map(Suggestion::suggestion).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("abcd", "abcde");
        List<Suggestion> sug2 = result.get(7);
        assertThat(sug2.get(0).suggestion()).isEqualTo("abcde");
        assertThat(sug2.get(0).score()).isEqualTo(1.0);
    }

}

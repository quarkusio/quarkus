package io.quarkus.redis.lettuce.runtime.internal.autosuggest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

@SuppressWarnings("ConstantConditions")
class LettuceTransactionalAutoSuggestCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void autoSuggestBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
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

        assertResult(result);
    }

    @Test
    void autoSuggestReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            var auto = tx.autosuggest();
            assertThat(auto.getDataSource()).isEqualTo(tx);
            return auto.ftSugAdd(key, "abc", 1.0)
                    .chain(() -> auto.ftSugAdd(key, "abcd", 1.0))
                    .chain(() -> auto.ftSugAdd(key, "abcde", 2.0))
                    .chain(() -> auto.ftSugAdd(key, "boo", 20))
                    .chain(() -> auto.ftSugDel(key, "boo"))
                    .chain(() -> auto.ftSugLen(key))
                    .chain(() -> auto.ftSugget(key, "abcd"))
                    .chain(() -> auto.ftSugget(key, "ab", new GetArgs().max(1).withScores()));
        }).await().indefinitely();

        assertResult(result);
    }

    private static void assertResult(TransactionResult result) {
        assertThat(result.size()).isEqualTo(8);
        assertThat(result.discarded()).isFalse();
        assertThat((Long) result.get(0)).isEqualTo(1);
        assertThat((Long) result.get(1)).isEqualTo(2);
        assertThat((Long) result.get(2)).isEqualTo(3);
        assertThat((Long) result.get(3)).isEqualTo(4);
        assertThat((Boolean) result.get(4)).isTrue();
        assertThat((Long) result.get(5)).isEqualTo(3);

        List<Suggestion> plain = result.get(6);
        assertThat(plain).hasSize(2);
        assertThat(plain).extracting(Suggestion::suggestion).containsExactlyInAnyOrder("abcd", "abcde");
        assertThat(plain).extracting(Suggestion::score).containsOnly(0.0);

        // MAX 1 WITHSCORES: one entry, with the score attached rather than returned as a second suggestion
        List<Suggestion> scored = result.get(7);
        assertThat(scored).hasSize(1);
        assertThat(scored.get(0).suggestion()).isEqualTo("abcde");
        assertThat(scored.get(0).score()).isEqualTo(1.0);
    }

}

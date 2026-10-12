package io.quarkus.redis.lettuce.runtime.internal.autosuggest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;

class LettuceAutoSuggestCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveAutoSuggestCommands<String> reactiveAuto;
    AutoSuggestCommands<String> blockingAuto;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveAuto = reactiveDs.autosuggest();
        blockingAuto = blockingDs.autosuggest();
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveAuto.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingAuto.getDataSource());
    }

    @Test
    void suggestions() {
        assertThat(blockingAuto.ftSugAdd(key, "hello world", 1)).isEqualTo(1L);
        assertThat(blockingAuto.ftSugAdd(key, "hello world", 3, true)).isEqualTo(1L);

        assertThat(blockingAuto.ftSugAdd(key, "bonjour", 3)).isEqualTo(2L);
        assertThat(blockingAuto.ftSugAdd(key, "bonjourno", 1)).isEqualTo(3L);

        assertThat(blockingAuto.ftSugLen(key)).isEqualTo(3L);
        assertThat(blockingAuto.ftSugDel(key, "bonjourno")).isTrue();
        assertThat(blockingAuto.ftSugDel(key, "missing")).isFalse();

        assertThat(blockingAuto.ftSugLen(key)).isEqualTo(2L);

        assertThat(blockingAuto.ftSugAdd(key, "hell", 3)).isEqualTo(3L);

        // Without WITHSCORES the score is 0.0, never null
        assertThat(blockingAuto.ftSugGet(key, "hell")).hasSize(2)
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hell"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hello world"))
                .allSatisfy(s -> assertThat(s.score()).isEqualTo(0.0));

        // WITHSCORES: exactly MAX entries, scores attached, no score leaking into the suggestion text
        assertThat(blockingAuto.ftSugGet(key, "hel", new GetArgs().max(1).withScores())).hasSize(1)
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hell");
                    assertThat(s.score()).isGreaterThan(0.0);
                });

        assertThat(blockingAuto.ftSugAdd(key, "hill", 3)).isEqualTo(4L);

        assertThat(blockingAuto.ftSugGet(key, "hell", new GetArgs().fuzzy())).hasSize(3)
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hell"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hello world"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hill"));

        assertThat(blockingAuto.ftSugGet(key, "hell", new GetArgs().fuzzy().withScores())).hasSize(3)
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hell");
                    assertThat(s.score()).isGreaterThan(0.0);
                })
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hello world");
                    assertThat(s.score()).isGreaterThan(0.0);
                })
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hill");
                    assertThat(s.score()).isGreaterThan(0.0);
                });
    }

    @Test
    void suggestionsReactive() {
        assertThat(reactiveAuto.ftSugAdd(key, "hello world", 1).await().atMost(TIMEOUT)).isEqualTo(1L);
        assertThat(reactiveAuto.ftSugAdd(key, "hell", 3).await().atMost(TIMEOUT)).isEqualTo(2L);
        assertThat(reactiveAuto.ftSugLen(key).await().atMost(TIMEOUT)).isEqualTo(2L);

        List<Suggestion> plain = reactiveAuto.ftSugGet(key, "hel").await().atMost(TIMEOUT);
        assertThat(plain).hasSize(2)
                .allSatisfy(s -> assertThat(s.score()).isEqualTo(0.0));

        List<Suggestion> scored = reactiveAuto.ftSugGet(key, "hel", new GetArgs().withScores()).await().atMost(TIMEOUT);
        assertThat(scored).hasSize(2)
                .allSatisfy(s -> assertThat(s.score()).isGreaterThan(0.0));

        assertThat(reactiveAuto.ftSugDel(key, "hell").await().atMost(TIMEOUT)).isTrue();
        assertThat(reactiveAuto.ftSugDel(key, "hell").await().atMost(TIMEOUT)).isFalse();
    }

    @Test
    void missingKeyReturnsEmptyList() {
        assertThat(blockingAuto.ftSugGet(key, "nothing")).isEmpty();
        assertThat(blockingAuto.ftSugGet(key, "nothing", new GetArgs().withScores())).isEmpty();
        assertThat(blockingAuto.ftSugLen(key)).isEqualTo(0L);
    }

    @Test
    void suggestionsWithTypeReference() {
        var auto = blockingDs.autosuggest(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });

        List<Person> key = List.of(Person.person0);

        assertThat(auto.ftSugAdd(key, "hello world", 1)).isEqualTo(1L);
        assertThat(auto.ftSugAdd(key, "hello world", 3, true)).isEqualTo(1L);

        assertThat(auto.ftSugAdd(key, "bonjour", 3)).isEqualTo(2L);
        assertThat(auto.ftSugAdd(key, "bonjourno", 1)).isEqualTo(3L);

        assertThat(auto.ftSugLen(key)).isEqualTo(3L);
        assertThat(auto.ftSugDel(key, "bonjourno")).isTrue();
        assertThat(auto.ftSugDel(key, "missing")).isFalse();

        assertThat(auto.ftSugLen(key)).isEqualTo(2L);

        assertThat(auto.ftSugAdd(key, "hell", 3)).isEqualTo(3L);

        assertThat(auto.ftSugGet(key, "hell")).hasSize(2)
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hell"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hello world"));

        assertThat(auto.ftSugGet(key, "hel", new GetArgs().max(1).withScores())).hasSize(1)
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hell");
                    assertThat(s.score()).isGreaterThan(0.0);
                });

        assertThat(auto.ftSugAdd(key, "hill", 3)).isEqualTo(4L);

        assertThat(auto.ftSugGet(key, "hell", new GetArgs().fuzzy())).hasSize(3)
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hell"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hello world"))
                .anySatisfy(s -> assertThat(s.suggestion()).isEqualTo("hill"));

        assertThat(auto.ftSugGet(key, "hell", new GetArgs().fuzzy().withScores())).hasSize(3)
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hell");
                    assertThat(s.score()).isGreaterThan(0.0);
                })
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hello world");
                    assertThat(s.score()).isGreaterThan(0.0);
                })
                .anySatisfy(s -> {
                    assertThat(s.suggestion()).isEqualTo("hill");
                    assertThat(s.score()).isGreaterThan(0.0);
                });
    }

}

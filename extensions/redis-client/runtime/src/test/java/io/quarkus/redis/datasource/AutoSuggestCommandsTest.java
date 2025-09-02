package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

/**
 * Lots of tests are using await().untilAsserted as the indexing process runs in the background.
 */
@RequiresCommand("ft.create")
public class AutoSuggestCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private AutoSuggestCommands<String> auto;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        auto = ds.autosuggest();
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(auto.getDataSource());
    }

    @Test
    void testSuggestions() {
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

    @Test
    void testSuggestionsWithTypeReference() {
        var auto = ds.autosuggest(new TypeReference<List<Person>>() {
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

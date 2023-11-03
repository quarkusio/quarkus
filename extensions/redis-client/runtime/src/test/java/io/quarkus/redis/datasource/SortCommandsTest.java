package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class SortCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-sort";
    private ListCommands<String, String> lists;
    private ValueCommands<String, String> strings;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(5));

        lists = ds.list(String.class);
        strings = ds.value(String.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void sort() {
        lists.rpush(key, "3", "2", "1");
        assertThat(lists.sort(key)).isEqualTo(List.of("1", "2", "3"));
        assertThat(lists.sort(key, new SortArgs().ascending())).isEqualTo(List.of("1", "2", "3"));
        assertThat(lists.sort(key, new SortArgs().descending())).isEqualTo(List.of("3", "2", "1"));
        assertThat(lists.sort(key, new SortArgs().descending().limit(0, 2))).isEqualTo(List.of("3", "2"));
    }

    @Test
    void sortAlpha() {
        lists.rpush(key, "A", "B", "C");
        assertThat(lists.sort(key, new SortArgs().alpha().descending())).isEqualTo(List.of("C", "B", "A"));
    }

    @Test
    void sortBy() {
        lists.rpush(key, "foo", "bar", "baz");
        strings.set("weight_foo", "8");
        strings.set("weight_bar", "4");
        strings.set("weight_baz", "2");
        assertThat(lists.sort(key, new SortArgs().by("weight_*"))).isEqualTo(List.of("baz", "bar", "foo"));
    }

    @Test
    void sortGet() {
        lists.rpush(key, "1", "2");
        strings.set("obj_1", "foo");
        strings.set("obj_2", "bar");
        assertThat(lists.sort(key, new SortArgs().get("obj_*"))).isEqualTo(List.of("foo", "bar"));
    }

    @Test
    void sortLimit() {
        lists.rpush(key, "3", "2", "1");
        assertThat(lists.sort(key, new SortArgs().limit(1, 2))).isEqualTo(List.of("2", "3"));
        assertThat(lists.sort(key, new SortArgs().limit(SortArgs.Limit.of(0, 2)))).isEqualTo(List.of("1", "2"));
    }

    @Test
    void sortStoreWithArgs() {
        lists.rpush("one", "1", "2", "3");
        assertThat(lists.sortAndStore("one", "two", new SortArgs().descending())).isEqualTo(3);
        assertThat(lists.lrange("two", 0, -1)).isEqualTo(List.of("3", "2", "1"));
    }

    @Test
    void sortStore() {
        lists.rpush("one", "1", "3", "2");
        assertThat(lists.sortAndStore("one", "two")).isEqualTo(3);
        assertThat(lists.lrange("two", 0, -1)).isEqualTo(List.of("1", "2", "3"));
    }

}

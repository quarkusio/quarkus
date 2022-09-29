package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class TopKCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private TopKCommands<String, Person> topk;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        topk = ds.topk(Person.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(topk.getDataSource());
    }

    @Test
    void addAndGet() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThatThrownBy(() -> topk.topkAdd(key, luke))
                .hasMessageContaining("TopK");

        topk.topkReserve(key, 2);
        assertThat(topk.topkAdd(key, luke)).isEmpty();
        assertThat(topk.topkAdd(key, luke)).isEmpty();
        assertThat(topk.topkAdd(key, leia)).isEmpty();
        assertThat(topk.topkAdd(key, anakin)).contains(leia);

        assertThat(topk.topkAdd(key, luke, luke, leia, leia, leia, luke)).containsExactly(null, null, anakin, null, null, null);

        assertThat(topk.topkList(key)).containsExactly(luke, leia);
        assertThat(topk.topkListWithCount(key)).contains(entry(luke, 5), entry(leia, 4));

        assertThat(topk.topkIncrBy(key, anakin, 6)).contains(leia);
        assertThat(topk.topkListWithCount(key)).contains(entry(anakin, 7), entry(luke, 5));

        assertThat(topk.topkIncrBy(key, Map.of(leia, 20, luke, 20))).hasSize(2);
        assertThat(topk.topkQuery(key, anakin)).isFalse();
        assertThat(topk.topkQuery(key, luke)).isTrue();
        assertThat(topk.topkQuery(key, luke, leia, anakin)).containsExactly(true, true, false);
    }

    @Test
    void creation() {
        topk.topkReserve(key, 10);
        topk.topkReserve(key + "1", 100, 2, 4, 0.5);
        assertThatThrownBy(() -> topk.topkReserve(key, 100, 3, 4, .01));
        assertThatThrownBy(() -> topk.topkReserve(key + "1", 20));
    }

}

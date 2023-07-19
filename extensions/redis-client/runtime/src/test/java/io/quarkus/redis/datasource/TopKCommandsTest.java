package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("topk.add")
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

    @Test
    void topKWithTypeReference() {
        var l1 = List.of(new Person("luke", "skywalker"), new Person("leia", "ordana"));
        var l2 = List.of(new Person("leia", "ordana"));
        var l3 = List.of(new Person("anakin", "skywalker"));

        var topk = ds.topk(new TypeReference<List<Person>>() {
            // Empty on purpose
        });

        assertThatThrownBy(() -> topk.topkAdd(key, l1))
                .hasMessageContaining("TopK");

        topk.topkReserve(key, 2);
        assertThat(topk.topkAdd(key, l1)).isEmpty();
        assertThat(topk.topkAdd(key, l1)).isEmpty();
        assertThat(topk.topkAdd(key, l2)).isEmpty();
        assertThat(topk.topkAdd(key, l3)).contains(l2);

        assertThat(topk.topkAdd(key, l1, l1, l2, l2, l2, l1)).containsExactly(null, null, l3, null, null, null);

        assertThat(topk.topkList(key)).containsExactly(l1, l2);
        assertThat(topk.topkListWithCount(key)).contains(entry(l1, 5), entry(l2, 4));

        assertThat(topk.topkIncrBy(key, l3, 6)).contains(l2);
        assertThat(topk.topkListWithCount(key)).contains(entry(l3, 7), entry(l1, 5));

        assertThat(topk.topkIncrBy(key, Map.of(l2, 20, l1, 20))).hasSize(2);
        assertThat(topk.topkQuery(key, l3)).isFalse();
        assertThat(topk.topkQuery(key, l1)).isTrue();
        assertThat(topk.topkQuery(key, l1, l2, l3)).containsExactly(true, true, false);
    }

}

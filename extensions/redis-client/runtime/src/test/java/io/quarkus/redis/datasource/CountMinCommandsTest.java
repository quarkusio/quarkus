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

import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("cms.query")
public class CountMinCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private CountMinCommands<String, Person> cm;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        cm = ds.countmin(Person.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(cm.getDataSource());
    }

    @Test
    void incrbyAndQuery() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        cm.cmsInitByDim(key, 10, 2);
        assertThat(cm.cmsIncrBy(key, leia, 10)).isEqualTo(10);
        assertThat(cm.cmsIncrBy(key, Map.of(leia, 2L, luke, 5L, anakin, 3L)))
                .contains(entry(leia, 12L), entry(luke, 5L), entry(anakin, 3L)).hasSize(3);

        assertThat(cm.cmsQuery(key, anakin)).isEqualTo(3);
        assertThat(cm.cmsQuery(key, leia, luke)).containsExactly(12L, 5L);
    }

    @Test
    void creation() {
        cm.cmsInitByDim(key, 10, 2);
        cm.cmsInitByProb(key + "1", 0.0001, 0.05);
        assertThatThrownBy(() -> cm.cmsInitByProb(key, 0.1, 0.2));
        assertThatThrownBy(() -> cm.cmsInitByDim(key + "1", 10, 2));
    }

    @Test
    void mergeWithWeights() {
        String key1 = key + "1";
        String key2 = key + "2";
        cm.cmsInitByDim(key1, 10, 2);
        cm.cmsInitByDim(key2, 10, 2);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        cm.cmsIncrBy(key1, leia, 2);
        cm.cmsIncrBy(key2, Map.of(leia, 2L, luke, 5L, anakin, 10L));

        cm.cmsInitByDim(key, 10, 2);
        cm.cmsMerge(key, List.of(key1, key2), List.of(2, 1));
        assertThat(cm.cmsQuery(key, anakin)).isEqualTo(10L);
        assertThat(cm.cmsQuery(key, leia)).isEqualTo(6L);
        assertThat(cm.cmsQuery(key, luke)).isEqualTo(5L);
    }

    @Test
    void mergeWithoutWeights() {
        String key1 = key + "1";
        String key2 = key + "2";
        cm.cmsInitByDim(key1, 10, 2);
        cm.cmsInitByDim(key2, 10, 2);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        cm.cmsIncrBy(key1, leia, 2);
        cm.cmsIncrBy(key2, Map.of(leia, 2L, luke, 5L, anakin, 10L));

        cm.cmsInitByDim(key, 10, 2);
        cm.cmsMerge(key, List.of(key1, key2), null);
        assertThat(cm.cmsQuery(key, anakin)).isEqualTo(10L);
        assertThat(cm.cmsQuery(key, leia)).isEqualTo(4L);
        assertThat(cm.cmsQuery(key, luke)).isEqualTo(5L);
    }

    @Test
    void countMinWithTypeReference() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        var cm = ds.countmin(new TypeReference<List<Person>>() {
            // Empty on purpose
        });

        cm.cmsInitByDim(key, 10, 2);
        assertThat(cm.cmsIncrBy(key, List.of(leia, luke), 10)).isEqualTo(10);
        assertThat(cm.cmsIncrBy(key, Map.of(List.of(leia, luke), 2L, List.of(luke, anakin), 5L, List.of(anakin), 3L)))
                .contains(entry(List.of(leia, luke), 12L), entry(List.of(luke, anakin), 5L), entry(List.of(anakin), 3L))
                .hasSize(3);

        assertThat(cm.cmsQuery(key, List.of(anakin))).isEqualTo(3);
        assertThat(cm.cmsQuery(key, List.of(leia, luke), List.of(luke, anakin))).containsExactly(12L, 5L);
    }

}

package io.quarkus.redis.lettuce.runtime.internal.countmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceCountMinCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveCountMinCommands<String, Person> reactiveCountMin;
    CountMinCommands<String, Person> blockingCountMin;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveCountMin = reactiveDs.countmin(Person.class);
        blockingCountMin = blockingDs.countmin(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveCountMin.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingCountMin.getDataSource());
    }

    @Test
    void incrbyAndQuery() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        blockingCountMin.cmsInitByDim(key, 10, 2);
        assertThat(blockingCountMin.cmsIncrBy(key, leia, 10)).isEqualTo(10);
        assertThat(blockingCountMin.cmsIncrBy(key, Map.of(leia, 2L, luke, 5L, anakin, 3L)))
                .contains(entry(leia, 12L), entry(luke, 5L), entry(anakin, 3L))
                .hasSize(3);

        assertThat(blockingCountMin.cmsQuery(key, anakin)).isEqualTo(3);
        assertThat(blockingCountMin.cmsQuery(key, leia, luke)).containsExactly(12L, 5L);
    }

    @Test
    void creation() {
        blockingCountMin.cmsInitByDim(key, 10, 2);
        blockingCountMin.cmsInitByProb(key + "1", 0.0001, 0.05);
        assertThatThrownBy(() -> blockingCountMin.cmsInitByProb(key, 0.1, 0.2));
        assertThatThrownBy(() -> blockingCountMin.cmsInitByDim(key + "1", 10, 2));
    }

    @Test
    void mergeWithWeights() {
        String key1 = key + "1";
        String key2 = key + "2";
        blockingCountMin.cmsInitByDim(key1, 10, 2);
        blockingCountMin.cmsInitByDim(key2, 10, 2);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        blockingCountMin.cmsIncrBy(key1, leia, 2);
        blockingCountMin.cmsIncrBy(key2, Map.of(leia, 2L, luke, 5L, anakin, 10L));

        blockingCountMin.cmsInitByDim(key, 10, 2);
        blockingCountMin.cmsMerge(key, List.of(key1, key2), List.of(2, 1));
        assertThat(blockingCountMin.cmsQuery(key, anakin)).isEqualTo(10L);
        assertThat(blockingCountMin.cmsQuery(key, leia)).isEqualTo(6L);
        assertThat(blockingCountMin.cmsQuery(key, luke)).isEqualTo(5L);
    }

    @Test
    void mergeWithoutWeights() {
        String key1 = key + "1";
        String key2 = key + "2";
        blockingCountMin.cmsInitByDim(key1, 10, 2);
        blockingCountMin.cmsInitByDim(key2, 10, 2);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        blockingCountMin.cmsIncrBy(key1, leia, 2);
        blockingCountMin.cmsIncrBy(key2, Map.of(leia, 2L, luke, 5L, anakin, 10L));

        blockingCountMin.cmsInitByDim(key, 10, 2);
        blockingCountMin.cmsMerge(key, List.of(key1, key2), null);
        assertThat(blockingCountMin.cmsQuery(key, anakin)).isEqualTo(10L);
        assertThat(blockingCountMin.cmsQuery(key, leia)).isEqualTo(4L);
        assertThat(blockingCountMin.cmsQuery(key, luke)).isEqualTo(5L);

        // An empty weight list behaves like no weights at all.
        String key3 = key + "3";
        blockingCountMin.cmsInitByDim(key3, 10, 2);
        blockingCountMin.cmsMerge(key3, List.of(key1, key2), List.of());
        assertThat(blockingCountMin.cmsQuery(key3, leia)).isEqualTo(4L);
    }

    @Test
    void mergeRejectsWeightCountMismatch() {
        String key1 = key + "1";
        String key2 = key + "2";
        blockingCountMin.cmsInitByDim(key1, 10, 2);
        blockingCountMin.cmsInitByDim(key2, 10, 2);

        Person leia = new Person("leia", "ordana");
        blockingCountMin.cmsIncrBy(key1, leia, 2);
        blockingCountMin.cmsIncrBy(key2, leia, 3);

        blockingCountMin.cmsInitByDim(key, 10, 2);

        // Fewer weights than sources
        assertThatThrownBy(() -> blockingCountMin.cmsMerge(key, List.of(key1, key2), List.of(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weight")
                .hasMessageContaining("2 expected")
                .hasMessageContaining("1 given");
        // More weights than sources
        assertThatThrownBy(() -> blockingCountMin.cmsMerge(key, List.of(key1, key2), List.of(2, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weight")
                .hasMessageContaining("2 expected")
                .hasMessageContaining("3 given");

        // The rejected merges never reached the server: the destination sketch is still empty.
        assertThat(blockingCountMin.cmsQuery(key, leia)).isEqualTo(0L);
    }

    @Test
    void mergeRejectsNullWeight() {
        String key1 = key + "1";
        String key2 = key + "2";
        blockingCountMin.cmsInitByDim(key1, 10, 2);
        blockingCountMin.cmsInitByDim(key2, 10, 2);

        Person leia = new Person("leia", "ordana");
        blockingCountMin.cmsIncrBy(key1, leia, 2);
        blockingCountMin.cmsIncrBy(key2, leia, 3);

        blockingCountMin.cmsInitByDim(key, 10, 2);

        List<Integer> weights = new ArrayList<>();
        weights.add(2);
        weights.add(null);
        assertThatThrownBy(() -> blockingCountMin.cmsMerge(key, List.of(key1, key2), weights))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weight");

        // The rejected merge never reached the server: the destination sketch is still empty.
        assertThat(blockingCountMin.cmsQuery(key, leia)).isEqualTo(0L);
    }

    @Test
    void incrbyRejectsNullCouple() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        blockingCountMin.cmsInitByDim(key, 10, 2);

        // A `null` item in the couples map
        Map<Person, Long> nullKey = new HashMap<>();
        nullKey.put(luke, 1L);
        nullKey.put(null, 2L);
        assertThatThrownBy(() -> blockingCountMin.cmsIncrBy(key, nullKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("couples");

        // A `null` increment in the couples map
        Map<Person, Long> nullValue = new HashMap<>();
        nullValue.put(luke, 1L);
        nullValue.put(leia, null);
        assertThatThrownBy(() -> blockingCountMin.cmsIncrBy(key, nullValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("couples");

        // The rejected calls never reached the server, so no partial increment was applied.
        assertThat(blockingCountMin.cmsQuery(key, luke, leia)).containsExactly(0L, 0L);
    }

    @Test
    void countMinWithTypeReference() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        var cm = blockingDs.countmin(new TypeReference<List<Person>>() {
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

    @Test
    void apiReturnsImmutableCollections() {
        blockingCountMin.cmsInitByDim(key, 100, 5);
        assertImmutable(blockingCountMin.cmsIncrBy(key, Map.of(Person.person1, 2L)));
        assertImmutable(blockingCountMin.cmsQuery(key, Person.person1, Person.person2));
    }

}

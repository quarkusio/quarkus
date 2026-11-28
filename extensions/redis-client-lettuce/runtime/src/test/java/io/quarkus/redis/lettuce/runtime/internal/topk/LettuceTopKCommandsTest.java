package io.quarkus.redis.lettuce.runtime.internal.topk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceTopKCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveTopKCommands<String, Person> reactiveTopK;
    TopKCommands<String, Person> blockingTopK;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveTopK = reactiveDs.topk(Person.class);
        blockingTopK = blockingDs.topk(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveTopK.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingTopK.getDataSource());
    }

    @Test
    void addAndGet() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThatThrownBy(() -> blockingTopK.topkAdd(key, luke))
                .hasMessageContaining("TopK");

        blockingTopK.topkReserve(key, 2);
        assertThat(blockingTopK.topkAdd(key, luke)).isEmpty();
        assertThat(blockingTopK.topkAdd(key, luke)).isEmpty();
        assertThat(blockingTopK.topkAdd(key, leia)).isEmpty();
        assertThat(blockingTopK.topkAdd(key, anakin)).contains(leia);
        assertThat(blockingTopK.topkAdd(key, luke, luke, leia, leia, leia, luke))
                .containsExactly(null, null, anakin, null, null, null);

        // TOPK.LIST replies most frequent first
        assertThat(blockingTopK.topkList(key)).containsExactly(luke, leia);
        assertThat(blockingTopK.topkListWithCount(key)).containsExactly(entry(luke, 5), entry(leia, 4));

        assertThat(blockingTopK.topkIncrBy(key, anakin, 6)).contains(leia);
        assertThat(blockingTopK.topkListWithCount(key)).containsExactly(entry(anakin, 7), entry(luke, 5));

        Map<Person, Integer> couples = new LinkedHashMap<>();
        couples.put(leia, 20);
        couples.put(luke, 20);
        // leia (20) evicts luke (5); luke (25) then evicts anakin (7)
        assertThat(blockingTopK.topkIncrBy(key, couples)).containsExactly(entry(leia, luke), entry(luke, anakin));

        assertThat(blockingTopK.topkQuery(key, anakin)).isFalse();
        assertThat(blockingTopK.topkQuery(key, luke)).isTrue();
        assertThat(blockingTopK.topkQuery(key, luke, leia, anakin)).containsExactly(true, true, false);
    }

    @Test
    void creation() {
        blockingTopK.topkReserve(key, 10);
        blockingTopK.topkReserve(key + "1", 100, 2, 4, 0.5);
        assertThatThrownBy(() -> blockingTopK.topkReserve(key, 100, 3, 4, .01));
        assertThatThrownBy(() -> blockingTopK.topkReserve(key + "1", 20));
    }

    @Test
    void topKWithTypeReference() {
        var l1 = List.of(new Person("luke", "skywalker"), new Person("leia", "ordana"));
        var l2 = List.of(new Person("leia", "ordana"));
        var l3 = List.of(new Person("anakin", "skywalker"));

        var topk = blockingDs.topk(new TypeReference<List<Person>>() {
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
        assertThat(topk.topkListWithCount(key)).containsExactly(entry(l1, 5), entry(l2, 4));

        assertThat(topk.topkIncrBy(key, l3, 6)).contains(l2);
        assertThat(topk.topkListWithCount(key)).containsExactly(entry(l3, 7), entry(l1, 5));
        assertThat(topk.topkIncrBy(key, Map.of(l2, 20, l1, 20))).hasSize(2);

        assertThat(topk.topkQuery(key, l3)).isFalse();
        assertThat(topk.topkQuery(key, l1)).isTrue();
        assertThat(topk.topkQuery(key, l1, l2, l3)).containsExactly(true, true, false);
    }

    @Test
    void apiReturnsImmutableCollections() {
        blockingTopK.topkReserve(key, 3);
        assertImmutable(blockingTopK.topkAdd(key, Person.person1, Person.person2));
        assertImmutable(blockingTopK.topkIncrBy(key, Map.of(Person.person1, 2)));
        assertImmutable(blockingTopK.topkList(key));
        assertImmutable(blockingTopK.topkListWithCount(key));
        assertImmutable(blockingTopK.topkQuery(key, Person.person1, Person.person3));
    }

}

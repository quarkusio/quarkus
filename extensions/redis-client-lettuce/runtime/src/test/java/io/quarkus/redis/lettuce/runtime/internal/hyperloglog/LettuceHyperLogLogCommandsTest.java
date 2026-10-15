package io.quarkus.redis.lettuce.runtime.internal.hyperloglog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceHyperLogLogCommandsTest extends CommandsTestBase {

    static AtomicInteger count = new AtomicInteger(0);

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveHyperLogLogCommands<String, Person> reactiveHyperLogLog;
    HyperLogLogCommands<String, Person> blockingHyperLogLog;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveHyperLogLog = reactiveDs.hyperloglog(Person.class);
        blockingHyperLogLog = blockingDs.hyperloglog(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveHyperLogLog.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingHyperLogLog.getDataSource());
    }

    @Test
    void pfadd() {
        String k = getKey();
        assertThat(blockingHyperLogLog.pfadd(k, Person.person1, Person.person1)).isTrue();
        assertThat(blockingHyperLogLog.pfadd(k, Person.person1, Person.person1)).isFalse();
        Assertions.assertThat(blockingHyperLogLog.pfadd(k, Person.person1)).isFalse();
    }

    @Test
    void pfaddNoValues() {
        assertThatThrownBy(() -> blockingHyperLogLog.pfadd(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    void pfaddNullValues() {
        assertThatThrownBy(() -> blockingHyperLogLog.pfadd(key, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`values`");

        assertThatThrownBy(() -> blockingHyperLogLog.pfadd(key, Person.person1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`values`");
    }

    private String getKey() {
        return "key-blockingHyperLogLog-" + count.getAndIncrement();
    }

    @Test
    void pfmerge() {
        String k1 = getKey();
        String k2 = getKey();
        String k3 = getKey();
        blockingHyperLogLog.pfadd(k1, Person.person1);
        blockingHyperLogLog.pfadd(k2, new Person("Bossk", ""));
        blockingHyperLogLog.pfadd(k3, new Person("Lando", "Calrissian"));

        blockingHyperLogLog.pfmerge(k1, k2, k3);
        assertThat(blockingHyperLogLog.pfcount(k1)).isEqualTo(3);

        String k4 = getKey();
        String k5 = getKey();
        blockingHyperLogLog.pfadd(k4, new Person("Lobot", ""), new Person("Ackbar", ""));
        blockingHyperLogLog.pfadd(k5, new Person("Ackbar", ""), new Person("Mon", "Mothma"));

        String k6 = getKey();
        blockingHyperLogLog.pfmerge(k6, k4, k5);

        assertThat(blockingHyperLogLog.pfcount(k6)).isEqualTo(3);
    }

    @Test
    void pfmergeNoKeys() {
        assertThatThrownBy(() -> blockingHyperLogLog.pfmerge(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfcount() {
        String k0 = getKey();
        String k1 = getKey();
        blockingHyperLogLog.pfadd(k0, Person.person1);
        blockingHyperLogLog.pfadd(k1, Person.person2);
        assertThat(blockingHyperLogLog.pfcount(k0)).isEqualTo(1);
        assertThat(blockingHyperLogLog.pfcount(k0, k1)).isEqualTo(2);
    }

    @Test
    void pfcountNoKeys() {
        assertThatThrownBy(() -> blockingHyperLogLog.pfcount()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddPfmergePfCount() {
        String k0 = getKey();
        String k1 = getKey();
        String k2 = getKey();
        blockingHyperLogLog.pfadd(k0, new Person("Lobot", ""), new Person("Ackbar", ""));
        blockingHyperLogLog.pfadd(k1, new Person("Ackbar", ""), new Person("Mon", "Mothma"));

        blockingHyperLogLog.pfmerge(k2, k0, k1);

        assertThat(blockingHyperLogLog.pfcount(k2)).isEqualTo(3);
    }

    @Test
    void pfaddWithTypeReference() {
        String k = getKey();
        var blockingHyperLogLog = blockingDs.hyperloglog(new TypeReference<List<Person>>() {
            // Empty on purpose
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person3, Person.person2);
        assertThat(blockingHyperLogLog.pfadd(k, l1, l2)).isTrue();
        assertThat(blockingHyperLogLog.pfadd(k, l1, l1)).isFalse();
        Assertions.assertThat(blockingHyperLogLog.pfadd(k, l1)).isFalse();
    }

}

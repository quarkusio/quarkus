package io.quarkus.redis.lettuce.runtime.internal.bloom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceBloomCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveBloomCommands<String, Person> reactiveBloom;
    BloomCommands<String, Person> blockingBloom;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveBloom = reactiveDs.bloom(Person.class);
        blockingBloom = blockingDs.bloom(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveBloom.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingBloom.getDataSource());
    }

    @Test
    void bfadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        assertThat(blockingBloom.bfadd(key, luke)).isTrue();

        assertThat(blockingBloom.bfexists(key, luke)).isTrue();
        assertThat(blockingBloom.bfexists(key, leia)).isFalse();

        assertThat(blockingBloom.bfadd(key, luke)).isFalse();
    }

    @Test
    void bfmadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(blockingBloom.bfmadd(key, luke, leia)).containsExactly(true, true);
        assertThat(blockingBloom.bfexists(key, luke)).isTrue();
        assertThat(blockingBloom.bfexists(key, leia)).isTrue();

        assertThat(blockingBloom.bfmexists(key, luke, anakin, leia)).containsExactly(true, false, true);
        assertThat(blockingBloom.bfmadd(key, luke, anakin)).containsExactly(false, true);
    }

    @Test
    void bfreserve() {
        blockingBloom.bfreserve(key, 0.0001, 600000);
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        assertThat(blockingBloom.bfmadd(key, luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingBloom.bfmexists(key, luke, anakin, leia)).containsExactly(true, true, true);

        assertThatThrownBy(() -> blockingBloom.bfreserve(key, 0.0001, 6000, new BfReserveArgs().nonScaling()));

        blockingBloom.bfreserve("key2", 0.0001, 6000, new BfReserveArgs().nonScaling());
        assertThat(blockingBloom.bfmadd("key2", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingBloom.bfmexists("key2", luke, anakin, leia)).containsExactly(true, true, true);

        blockingBloom.bfreserve("key3", 0.0001, 6000, new BfReserveArgs().expansion(5));
        assertThat(blockingBloom.bfmadd("key3", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingBloom.bfmexists("key3", luke, anakin, leia)).containsExactly(true, true, true);
    }

    @Test
    void bfinsert() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(blockingBloom.bfinsert(key, new BfInsertArgs().capacity(600000).errorRate(0.0001).nonScaling(), luke, leia))
                .containsExactly(true, true);

        assertThat(blockingBloom.bfexists(key, luke)).isTrue();
        assertThat(blockingBloom.bfexists(key, leia)).isTrue();
        assertThat(blockingBloom.bfexists(key, anakin)).isFalse();
        assertThat(blockingBloom.bfadd(key, luke)).isFalse();

        assertThatThrownBy(() -> blockingBloom.bfinsert("key2",
                new BfInsertArgs().capacity(600000).errorRate(0.0001).nonScaling().nocreate(), luke, leia, anakin));
        assertThatThrownBy(
                () -> blockingBloom.bfinsert("key3", new BfInsertArgs().capacity(600000).errorRate(0.0001).expansion(1)));
    }

    @Test
    void bfmaddOnFullNonScalingFilterNeverReturnsNull() {
        blockingBloom.bfreserve(key, 0.01, 2, new BfReserveArgs().nonScaling());

        List<Boolean> added = blockingBloom.bfmadd(key, people(8));
        assertThat(added).isNotEmpty().doesNotContainNull().contains(true, false);
        assertThat(added.get(added.size() - 1)).isFalse();
    }

    @Test
    void bfinsertOnFullNonScalingFilterNeverReturnsNull() {
        List<Boolean> added = blockingBloom.bfinsert(key, new BfInsertArgs().capacity(2).errorRate(0.01).nonScaling(),
                people(8));
        assertThat(added).isNotEmpty().doesNotContainNull().contains(true, false);
        assertThat(added.get(added.size() - 1)).isFalse();

        List<Boolean> again = reactiveBloom.bfinsert(key, new BfInsertArgs().nonScaling(), people(3))
                .await().atMost(TIMEOUT);
        assertThat(again).isNotEmpty().doesNotContainNull().containsOnly(false);
    }

    private static Person[] people(int count) {
        Person[] people = new Person[count];
        for (int i = 0; i < count; i++) {
            people[i] = new Person("person-" + i, "nonscaling");
        }
        return people;
    }

    @Test
    void bloomWithTypeReference() {
        var bloom = blockingDs.bloom(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);
        assertThat(bloom.bfadd(key, l1)).isTrue();

        assertThat(bloom.bfexists(key, l1)).isTrue();
        assertThat(bloom.bfexists(key, l2)).isFalse();

        assertThat(bloom.bfadd(key, l1)).isFalse();
        assertThat(bloom.bfadd(key, l2)).isTrue();
    }

    @Test
    void apiReturnsImmutableCollections() {
        assertImmutable(blockingBloom.bfmadd(key, Person.person1, Person.person2));
        assertImmutable(blockingBloom.bfmexists(key, Person.person1, Person.person3));
        assertImmutable(blockingBloom.bfinsert(key, new BfInsertArgs(), Person.person0));
    }

}

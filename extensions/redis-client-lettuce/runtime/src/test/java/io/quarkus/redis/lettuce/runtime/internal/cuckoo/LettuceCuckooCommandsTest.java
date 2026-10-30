package io.quarkus.redis.lettuce.runtime.internal.cuckoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceCuckooCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveCuckooCommands<String, Person> reactiveCuckoo;
    CuckooCommands<String, Person> blockingCuckoo;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveCuckoo = reactiveDs.cuckoo(Person.class);
        blockingCuckoo = blockingDs.cuckoo(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveCuckoo.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingCuckoo.getDataSource());
    }

    @Test
    void cfadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        blockingCuckoo.cfadd(key, luke);
        blockingCuckoo.cfadd(key, luke);
        blockingCuckoo.cfadd(key, leia);

        assertThat(blockingCuckoo.cfexists(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, leia)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, anakin)).isFalse();

        assertThat(blockingCuckoo.cfaddnx(key, leia)).isFalse();
        assertThat(blockingCuckoo.cfaddnx(key, anakin)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, anakin)).isTrue();

        assertThat(blockingCuckoo.cfcount(key, leia)).isEqualTo(1);
        assertThat(blockingCuckoo.cfcount(key, luke)).isEqualTo(2);
        assertThat(blockingCuckoo.cfcount(key, anakin)).isEqualTo(1);
        assertThat(blockingCuckoo.cfcount(key, new Person("john", "doe"))).isEqualTo(0);
    }

    @Test
    void cfdel() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        blockingCuckoo.cfadd(key, luke);
        blockingCuckoo.cfadd(key, luke);
        blockingCuckoo.cfadd(key, leia);

        assertThat(blockingCuckoo.cfexists(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, leia)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, anakin)).isFalse();
        assertThat(blockingCuckoo.cfmexists(key, leia, luke, anakin)).containsExactly(true, true, false);

        assertThat(blockingCuckoo.cfdel(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfdel(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfdel(key, luke)).isFalse();

        assertThat(blockingCuckoo.cfdel(key, anakin)).isFalse();

        assertThatThrownBy(() -> blockingCuckoo.cfdel("missing", anakin))
                .hasMessageContaining("Not found");
    }

    @Test
    void cfinsert() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        blockingCuckoo.cfinsert(key, new CfInsertArgs().capacity(600000), luke, leia, leia);

        assertThat(blockingCuckoo.cfexists(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, leia)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, anakin)).isFalse();
        assertThat(blockingCuckoo.cfcount(key, leia)).isEqualTo(2);
        assertThat(blockingCuckoo.cfcount(key, luke)).isEqualTo(1);
        assertThat(blockingCuckoo.cfaddnx(key, luke)).isFalse();

        assertThatThrownBy(() -> blockingCuckoo.cfinsert("key2",
                new CfInsertArgs().capacity(600000).nocreate(), luke, leia, anakin));
        assertThatThrownBy(() -> blockingCuckoo.cfinsert("key3", new CfInsertArgs().capacity(600000)));
    }

    @Test
    void cfinsertnx() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(blockingCuckoo.cfinsertnx(key, new CfInsertArgs().capacity(600000), luke, leia, leia))
                .containsExactly(true, true, false);

        assertThat(blockingCuckoo.cfexists(key, luke)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, leia)).isTrue();
        assertThat(blockingCuckoo.cfexists(key, anakin)).isFalse();
        assertThat(blockingCuckoo.cfcount(key, leia)).isEqualTo(1);
        assertThat(blockingCuckoo.cfcount(key, luke)).isEqualTo(1);
        assertThat(blockingCuckoo.cfaddnx(key, luke)).isFalse();

        assertThatThrownBy(() -> blockingCuckoo.cfinsertnx("key2",
                new CfInsertArgs().capacity(600000).nocreate(), luke, leia, anakin));
        assertThatThrownBy(() -> blockingCuckoo.cfinsertnx("key3", new CfInsertArgs().capacity(600000)));
    }

    @Test
    void cfreserve() {
        blockingCuckoo.cfreserve(key, 600000);
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        assertThat(blockingCuckoo.cfinsertnx(key, luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingCuckoo.cfmexists(key, luke, anakin, leia)).containsExactly(true, true, true);

        assertThatThrownBy(() -> blockingCuckoo.cfreserve(key, 6000, new CfReserveArgs().bucketSize(10)));

        blockingCuckoo.cfreserve("key2", 6000, new CfReserveArgs().maxIterations(1));
        assertThat(blockingCuckoo.cfinsertnx("key2", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingCuckoo.cfmexists("key2", luke, anakin, leia)).containsExactly(true, true, true);

        blockingCuckoo.cfreserve("key3", 6000, new CfReserveArgs().expansion(5).bucketSize(1));
        assertThat(blockingCuckoo.cfinsertnx("key3", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(blockingCuckoo.cfmexists("key3", luke, anakin, leia)).containsExactly(true, true, true);
    }

    @Test
    void cuckooWithTypeReference() {
        var cuckoo = blockingDs.cuckoo(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);
        cuckoo.cfadd(key, l1);

        assertThat(cuckoo.cfexists(key, l1)).isTrue();
        assertThat(cuckoo.cfexists(key, l2)).isFalse();

        assertThat(cuckoo.cfaddnx(key, l1)).isFalse();
        cuckoo.cfadd(key, l2);
        assertThat(cuckoo.cfexists(key, l2)).isTrue();
    }

    @Test
    void cfinsertnxOnNonGrowingFilterNeverReturnsNull() {
        // Quarkus' CfReserveArgs cannot express EXPANSION 0, so reserve the non-growing filter with a raw command.
        blockingDs.execute("CF.RESERVE", key, "2", "BUCKETSIZE", "1", "MAXITERATIONS", "1", "EXPANSION", "0");

        // CF.INSERTNX answers -1 for every item the full filter cannot place; the Vert.x backend reports those as false.
        List<Boolean> added = blockingCuckoo.cfinsertnx(key, people(8));
        assertThat(added).hasSize(8).doesNotContainNull().contains(true, false);
        assertThat(added.get(added.size() - 1)).isFalse();
    }

    @Test
    void cfinsertnxWithArgsOnNonGrowingFilterNeverReturnsNull() {
        blockingDs.execute("CF.RESERVE", key, "2", "BUCKETSIZE", "1", "MAXITERATIONS", "1", "EXPANSION", "0");

        List<Boolean> added = blockingCuckoo.cfinsertnx(key, new CfInsertArgs().nocreate(), people(8));
        assertThat(added).hasSize(8).doesNotContainNull().contains(true, false);

        // Re-inserting: the items already present answer 0, the ones that never fitted answer -1 -> all false, no nulls.
        List<Boolean> again = reactiveCuckoo.cfinsertnx(key, new CfInsertArgs().nocreate(), people(8))
                .await().atMost(TIMEOUT);
        assertThat(again).hasSize(8).doesNotContainNull().containsOnly(false);
    }

    private static Person[] people(int count) {
        Person[] people = new Person[count];
        for (int i = 0; i < count; i++) {
            people[i] = new Person("person" + i, "lastname" + i);
        }
        return people;
    }

    @Test
    void apiReturnsImmutableCollections() {
        assertImmutable(blockingCuckoo.cfinsertnx(key, Person.person1, Person.person2));
        assertImmutable(blockingCuckoo.cfinsertnx(key, new CfInsertArgs(), Person.person3));
        assertImmutable(blockingCuckoo.cfmexists(key, Person.person1, Person.person0));
    }

}

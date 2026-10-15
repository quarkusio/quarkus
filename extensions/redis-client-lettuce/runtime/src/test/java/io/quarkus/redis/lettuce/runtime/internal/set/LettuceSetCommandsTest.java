package io.quarkus.redis.lettuce.runtime.internal.set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.set.SScanCursor;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceSetCommandsTest extends CommandsTestBase {

    static final Person person1 = new Person("luke", "skywalker");
    static final Person person2 = new Person("anakin", "skywalker");
    static final Person person3 = new Person("greedo", "");
    static final Person person4 = new Person("jabba", "desilijic tiure");
    static final Person person5 = new Person("wedge", "antilles");

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveSetCommands<String, Person> reactiveSets;
    SetCommands<String, Person> blockingSets;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveSets = reactiveDs.set(Person.class);
        blockingSets = blockingDs.set(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveSets.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingSets.getDataSource());
    }

    @Test
    void sadd() {
        assertThat(blockingSets.sadd(key, person1)).isEqualTo(1L);
        assertThat(blockingSets.sadd(key, person1)).isEqualTo(0);
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person1));
        assertThat(blockingSets.sadd(key, person2, person3)).isEqualTo(2);
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person1, person2, person3));
    }

    @Test
    void scard() {
        assertThat(blockingSets.scard(key)).isEqualTo(0);
        blockingSets.sadd(key, person1);
        assertThat((long) blockingSets.scard(key)).isEqualTo(1);
    }

    @Test
    void sdiff() {
        populate();
        assertThat(blockingSets.sdiff("key1", "key2", "key3")).isEqualTo(Set.of(person2, person4));
    }

    @Test
    void sdiffstore() {
        populate();
        assertThat(blockingSets.sdiffstore("newset", "key1", "key2", "key3")).isEqualTo(2);
        assertThat(blockingSets.smembers("newset")).containsOnly(person2, person4);
    }

    @Test
    void sinter() {
        populate();
        assertThat(blockingSets.sinter("key1", "key2", "key3")).isEqualTo(Set.of(person3));
        assertThat(blockingSets.sintercard("key1", "key2", "key3")).isEqualTo(1L);
        assertThat(blockingSets.sintercard(2, "key1", "key2", "key3")).isEqualTo(1L);
    }

    @Test
    void sinterstore() {
        populate();
        assertThat(blockingSets.sinterstore("newset", "key1", "key2", "key3")).isEqualTo(1);
        assertThat(blockingSets.smembers("newset")).containsExactly(person3);
    }

    @Test
    void sismember() {
        assertThat(blockingSets.sismember(key, person1)).isFalse();
        blockingSets.sadd(key, person1);
        assertThat(blockingSets.sismember(key, person1)).isTrue();
    }

    @Test
    void smove() {
        blockingSets.sadd(key, person1, person2, person3);
        assertThat(blockingSets.smove(key, "key1", person4)).isFalse();
        assertThat(blockingSets.smove(key, "key1", person1)).isTrue();
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person2, person3));
        assertThat(blockingSets.smembers("key1")).isEqualTo(Set.of(person1));
    }

    @Test
    void smembers() {
        populate();
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person1, person2, person3));
    }

    @Test
    void smembersOnMissingKey() {
        assertThat(blockingSets.smembers("missing")).isEmpty();
    }

    @Test
    void smismember() {
        assertThat(blockingSets.smismember(key, person1)).isEqualTo(List.of(false));
        blockingSets.sadd(key, person1);
        assertThat(blockingSets.smismember(key, person1)).isEqualTo(List.of(true));
        assertThat(blockingSets.smismember(key, person2, person1)).isEqualTo(List.of(false, true));
    }

    @Test
    void spop() {
        assertThat(blockingSets.spop(key)).isNull();
        blockingSets.sadd(key, person1, person2, person3);
        Person rand = blockingSets.spop(key);
        assertThat(Set.of(person1, person2, person3).contains(rand)).isTrue();
        assertThat(blockingSets.smembers(key).contains(rand)).isFalse();
    }

    @Test
    void spopMultiple() {
        assertThat(blockingSets.spop(key, 2)).isEmpty();
        blockingSets.sadd(key, person1, person2, person3);
        Set<Person> rand = blockingSets.spop(key, 2);
        assertThat(rand).hasSize(2);
        assertThat(Set.of(person1, person2, person3).containsAll(rand)).isTrue();
        assertThat(blockingSets.scard(key)).isEqualTo(1);
    }

    @Test
    void srandmember() {
        assertThat(blockingSets.srandmember(key)).isNull();
        assertThat(blockingSets.srandmember(key, 3)).isEmpty();

        blockingSets.sadd(key, person1, person2, person3, person4);
        assertThat(Set.of(person1, person2, person3, person4).contains(blockingSets.srandmember(key))).isTrue();
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person1, person2, person3, person4));
        List<Person> rand = blockingSets.srandmember(key, 3);
        assertThat(rand).hasSize(3);
        assertThat(Set.of(person1, person2, person3, person4).containsAll(rand)).isTrue();
        // A negative count is a legal SRANDMEMBER request for duplicates, so it is not validated.
        List<Person> randWithDuplicates = blockingSets.srandmember(key, -10);
        assertThat(randWithDuplicates).hasSize(10);
    }

    @Test
    void srem() {
        blockingSets.sadd(key, person1, person2, person3);
        assertThat(blockingSets.srem(key, person4)).isEqualTo(0);
        assertThat(blockingSets.srem(key, person2)).isEqualTo(1);
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of(person1, person3));
        assertThat(blockingSets.srem(key, person1, person3)).isEqualTo(2);
        assertThat(blockingSets.smembers(key)).isEqualTo(Set.of());
    }

    @Test
    void sremEmpty() {
        assertThatThrownBy(() -> blockingSets.srem(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    void sremNulls() {
        assertThatThrownBy(() -> blockingSets.srem(key, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sunion() {
        populate();
        assertThat(blockingSets.sunion("key1", "key2", "key3"))
                .isEqualTo(Set.of(person1, person2, person3, person4, person5));
    }

    @Test
    void sunionEmpty() {
        assertThatThrownBy(() -> blockingSets.sunion()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sunionstore() {
        populate();
        assertThat(blockingSets.sunionstore("newset", "key1", "key2", "key3")).isEqualTo(5);
        assertThat(blockingSets.smembers("newset")).isEqualTo(Set.of(person1, person2, person3, person4, person5));
    }

    @Test
    void sscan() {
        blockingSets.sadd(key, person1);
        SScanCursor<Person> cursor = blockingSets.sscan(key);

        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(list).hasSize(1).containsExactly(person1);
    }

    @Test
    void sscanEmpty() {
        SScanCursor<Person> cursor = blockingSets.sscan(key);

        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.hasNext()).isFalse();
        assertThat(list).isEmpty();
    }

    @Test
    void sscanEmptyAsIterable() {
        SScanCursor<Person> cursor = blockingSets.sscan(key);

        assertThat(cursor.hasNext()).isTrue();

        Iterable<Person> iterable = cursor.toIterable();
        assertThat(iterable).isEmpty();
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void sscanWithCursorAndArgs() {
        blockingSets.sadd(key, person1);
        SScanCursor<Person> cursor = blockingSets.sscan(key, new ScanArgs().count(3));

        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.hasNext()).isFalse();
        assertThat(list).hasSize(1).containsExactly(person1);
    }

    @Test
    void sscanMultiple() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = blockingDs.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(5));
        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        assertThat(check).containsExactlyInAnyOrderElementsOf(expect);
    }

    @Test
    void sscanMultipleAsIterable() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = blockingDs.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(5));
        Iterable<String> iterable = cursor.toIterable();
        for (String s : iterable) {
            check.add(s);
        }

        assertThat(check).containsExactlyInAnyOrderElementsOf(expect);
    }

    @Test
    void sscanMatch() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = blockingDs.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(200).match("hello1*"));
        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        // hello1 plus hello10..hello19
        assertThat(check).hasSize(11);
    }

    @Test
    void sscanReactiveAsMulti() {
        Set<String> expect = new HashSet<>();
        populateMany(expect, blockingDs.set(String.class, String.class));

        ReactiveSetCommands<String, String> set = reactiveDs.set(String.class, String.class);
        ReactiveSScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(5));
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isTrue();

        List<String> found = cursor.toMulti().collect().asList().await().atMost(TIMEOUT);

        assertThat(found).containsExactlyInAnyOrderElementsOf(expect);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0);
    }

    @Test
    void sort() {
        SetCommands<String, String> commands = blockingDs.set(String.class, String.class);
        commands.sadd(key, "9", "5", "1", "3", "5", "8", "7", "6", "2", "4");

        assertThat(commands.sort(key)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");

        assertThat(commands.sort(key, new SortArgs().descending())).containsExactly("9", "8", "7", "6", "5", "4", "3",
                "2", "1");

        String k = key + "-alpha";
        commands.sadd(k, "a", "e", "f", "b");

        assertThat(commands.sort(k, new SortArgs().alpha())).containsExactly("a", "b", "e", "f");
        assertThat(commands.sort(k, new SortArgs().alpha().limit(1, 2))).containsExactly("b", "e");

        commands.sortAndStore(k, "dest1", new SortArgs().alpha());
        commands.sortAndStore(key, "dest2");

        ListCommands<String, String> listCommands = blockingDs.list(String.class, String.class);
        assertThat(listCommands.lrange("dest1", 0, -1)).containsExactly("a", "b", "e", "f");
        assertThat(listCommands.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void sortOnMissingKey() {
        assertThat(blockingSets.sort("missing")).isEmpty();
    }

    @Test
    void sortAndStore() {
        SetCommands<String, String> commands = blockingDs.set(String.class, String.class);
        String alphaKey = key + "-alpha";
        commands.sadd(key, "9", "5", "1", "3", "8", "7", "6", "2", "4");
        commands.sadd(alphaKey, "a", "e", "f", "b");

        assertThat(commands.sortAndStore(alphaKey, "dest1", new SortArgs().alpha())).isEqualTo(4);
        assertThat(commands.sortAndStore(key, "dest2")).isEqualTo(9);
    }

    @Test
    void testSetWithTypeReference() {
        var sets = blockingDs.set(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        assertThat(sets.sadd(key, List.of(person1, person2))).isEqualTo(1L);
        assertThat(sets.sadd(key, List.of(person1, person2))).isEqualTo(0);
        assertThat(sets.smembers(key)).isEqualTo(Set.of(List.of(person1, person2)));
        assertThat(sets.sadd(key, List.of(person2, person3), List.of(person4))).isEqualTo(2);
        assertThat(sets.smembers(key)).containsExactlyInAnyOrder(List.of(person1, person2), List.of(person2, person3),
                List.of(person4));
    }

    /** Covers the {@code TypeReference} overloads the Vert.x-mirrored test above does not reach. */
    @Test
    void setWithKeyAndMemberTypeReferences() {
        SetCommands<String, String> memberOnly = blockingDs.set(new TypeReference<>() {
            // Empty on purpose
        });
        assertThat(memberOnly.sadd(key, "a", "b")).isEqualTo(2);

        SetCommands<String, String> keyAndMember = blockingDs.set(new TypeReference<>() {
            // Empty on purpose
        }, new TypeReference<>() {
            // Empty on purpose
        });
        assertThat(keyAndMember.smembers(key)).containsExactlyInAnyOrder("a", "b");

        ReactiveSetCommands<String, String> reactiveCommands = reactiveDs.set(new TypeReference<>() {
            // Empty on purpose
        });
        assertThat(reactiveCommands.scard(key).await().atMost(TIMEOUT)).isEqualTo(2L);
    }

    @Test
    void invalidCountsAreRejected() {
        assertThatThrownBy(() -> blockingSets.spop(key, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("count");
        assertThatThrownBy(() -> blockingSets.sintercard(0, "key1", "key2"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limit");
        assertThatThrownBy(() -> blockingSets.sintercard(-1, "key1", "key2"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limit");
    }

    /** Building the {@code Uni} is fine; only subscribing surfaces the single-key failure. */
    @Test
    void singleKeyFailureIsDeferredUntilSubscription() {
        var uni = reactiveSets.sdiff("key1");
        assertThatThrownBy(() -> uni.await().atMost(TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 2 keys");
    }

    void populateMany(Set<String> expect, SetCommands<String, String> sets) {
        for (int i = 0; i < 100; i++) {
            sets.sadd(key, "hello" + i);
            expect.add("hello" + i);
        }
    }

    private void populate() {
        blockingSets.sadd(key, person1, person2, person3);
        blockingSets.sadd("key1", person1, person2, person3, person4);
        blockingSets.sadd("key2", person3);
        blockingSets.sadd("key3", person1, person3, person5);
    }

}

package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.set.SScanCursor;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class SetCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;
    private static final Person person1 = new Person("luke", "skywalker");
    private static final Person person2 = new Person("anakin", "skywalker");
    private static final Person person3 = new Person("greedo", "");
    private static final Person person4 = new Person("jabba", "desilijic tiure");
    private static final Person person5 = new Person("wedge", "antilles");

    static String key = "key-set";
    private SetCommands<String, Person> sets;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));

        sets = ds.set(Person.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(sets.getDataSource());
    }

    @Test
    void sadd() {
        assertThat(sets.sadd(key, person1)).isEqualTo(1L);
        assertThat(sets.sadd(key, person1)).isEqualTo(0);
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person1));
        assertThat(sets.sadd(key, person2, person3)).isEqualTo(2);
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person1, person2, person3));
    }

    @Test
    void scard() {
        assertThat(sets.scard(key)).isEqualTo(0);
        sets.sadd(key, person1);
        assertThat((long) sets.scard(key)).isEqualTo(1);
    }

    @Test
    void sdiff() {
        populate();
        assertThat(sets.sdiff("key1", "key2", "key3")).isEqualTo(Set.of(person2, person4));
    }

    @Test
    void sdiffstore() {
        populate();
        assertThat(sets.sdiffstore("newset", "key1", "key2", "key3")).isEqualTo(2);
        assertThat(sets.smembers("newset")).containsOnly(person2, person4);
    }

    @Test
    @RequiresRedis7OrHigher // because of sintercard
    void sinter() {
        populate();
        assertThat(sets.sinter("key1", "key2", "key3")).isEqualTo(Set.of(person3));
        assertThat(sets.sintercard("key1", "key2", "key3")).isEqualTo(1L);
        assertThat(sets.sintercard(2, "key1", "key2", "key3")).isEqualTo(1L);
    }

    @Test
    void sinterstore() {
        populate();
        assertThat(sets.sinterstore("newset", "key1", "key2", "key3")).isEqualTo(1);
        assertThat(sets.smembers("newset")).containsExactly(person3);
    }

    @Test
    void sismember() {
        assertThat(sets.sismember(key, person1)).isFalse();
        sets.sadd(key, person1);
        assertThat(sets.sismember(key, person1)).isTrue();
    }

    @Test
    void smove() {
        sets.sadd(key, person1, person2, person3);
        assertThat(sets.smove(key, "key1", person4)).isFalse();
        assertThat(sets.smove(key, "key1", person1)).isTrue();
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person2, person3));
        assertThat(sets.smembers("key1")).isEqualTo(Set.of(person1));
    }

    @Test
    void smembers() {
        populate();
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person1, person2, person3));
    }

    @Test
    @RequiresRedis6OrHigher
    void smismember() {
        assertThat(sets.smismember(key, person1)).isEqualTo(List.of(false));
        sets.sadd(key, person1);
        assertThat(sets.smismember(key, person1)).isEqualTo(List.of(true));
        assertThat(sets.smismember(key, person2, person1)).isEqualTo(List.of(false, true));
    }

    @Test
    void spop() {
        assertThat(sets.spop(key)).isNull();
        sets.sadd(key, person1, person2, person3);
        Person rand = sets.spop(key);
        assertThat(Set.of(person1, person2, person3).contains(rand)).isTrue();
        assertThat(sets.smembers(key).contains(rand)).isFalse();
    }

    @Test
    void spopMultiple() {
        assertThat(sets.spop(key)).isNull();
        sets.sadd(key, person1, person2, person3);
        Set<Person> rand = sets.spop(key, 2);
        assertThat(rand).hasSize(2);
        assertThat(Set.of(person1, person2, person3).containsAll(rand)).isTrue();
    }

    @Test
    void srandmember() {
        assertThat(sets.spop(key)).isNull();
        sets.sadd(key, person1, person2, person3, person4);
        assertThat(Set.of(person1, person2, person3, person4).contains(sets.srandmember(key))).isTrue();
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person1, person2, person3, person4));
        List<Person> rand = sets.srandmember(key, 3);
        assertThat(rand).hasSize(3);
        assertThat(Set.of(person1, person2, person3, person4).containsAll(rand)).isTrue();
        List<Person> randWithDuplicates = sets.srandmember(key, -10);
        assertThat(randWithDuplicates).hasSize(10);
    }

    @Test
    void srem() {
        sets.sadd(key, person1, person2, person3);
        assertThat(sets.srem(key, person4)).isEqualTo(0);
        assertThat(sets.srem(key, person2)).isEqualTo(1);
        assertThat(sets.smembers(key)).isEqualTo(Set.of(person1, person3));
        assertThat(sets.srem(key, person1, person3)).isEqualTo(2);
        assertThat(sets.smembers(key)).isEqualTo(Set.of());
    }

    @Test
    void sremEmpty() {
        assertThatThrownBy(() -> sets.srem(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    void sremNulls() {
        assertThatThrownBy(() -> sets.srem(key, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sunion() {
        populate();
        assertThat(sets.sunion("key1", "key2", "key3")).isEqualTo(Set.of(person1, person2, person3, person4, person5));
    }

    @Test
    void sunionEmpty() {
        assertThatThrownBy(() -> sets.sunion()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sunionstore() {
        populate();
        assertThat(sets.sunionstore("newset", "key1", "key2", "key3")).isEqualTo(5);
        assertThat(sets.smembers("newset")).isEqualTo(Set.of(person1, person2, person3, person4, person5));
    }

    @Test
    void sscan() {
        sets.sadd(key, person1);
        SScanCursor<Person> cursor = sets.sscan(key);

        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(list).hasSize(1).containsExactly(person1);
    }

    @Test
    void sscanEmpty() {
        SScanCursor<Person> cursor = sets.sscan(key);

        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(list).isEmpty();
    }

    @Test
    void sscanEmptyAsIterable() {
        SScanCursor<Person> cursor = sets.sscan(key);

        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();

        Iterable<Person> iterable = cursor.toIterable();
        assertThat(iterable).isEmpty();
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void sscanWithCursorAndArgs() {
        sets.sadd(key, person1);
        SScanCursor<Person> cursor = sets.sscan(key, new ScanArgs().count(3));

        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();

        List<Person> list = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(list).hasSize(1).containsExactly(person1);

    }

    @Test
    void sscanMultiple() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = ds.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(5));
        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        assertThat(check).containsExactlyInAnyOrderElementsOf(expect);
    }

    @Test
    void sscanMultipleAsITerable() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = ds.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(5));
        Iterable<String> iterable = cursor.toIterable();
        for (String s : iterable) {
            check.add(s);
        }

        assertThat(check).containsExactlyInAnyOrderElementsOf(expect);
    }

    @Test
    void scanMatch() {
        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        SetCommands<String, String> set = ds.set(String.class, String.class);
        populateMany(expect, set);

        SScanCursor<String> cursor = set.sscan(key, new ScanArgs().count(200).match("hello1*"));
        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        assertThat(check).hasSize(11);
    }

    void populateMany(Set<String> expect, SetCommands<String, String> sets) {
        for (int i = 0; i < 100; i++) {
            sets.sadd(key, "hello" + i);
            expect.add("hello" + i);
        }
    }

    private void populate() {
        sets.sadd(key, person1, person2, person3);
        sets.sadd("key1", person1, person2, person3, person4);
        sets.sadd("key2", person3);
        sets.sadd("key3", person1, person3, person5);
    }

    @Test
    @RequiresRedis6OrHigher
    void sort() {
        SetCommands<String, String> commands = ds.set(String.class, String.class);
        commands.sadd(key, "9", "5", "1", "3", "5", "8", "7", "6", "2", "4");

        assertThat(commands.sort(key)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");

        assertThat(commands.sort(key, new SortArgs().descending())).containsExactly("9", "8", "7", "6", "5", "4", "3", "2",
                "1");

        String k = key + "-alpha";
        commands.sadd(k, "a", "e", "f", "b");

        assertThat(commands.sort(k, new SortArgs().alpha())).containsExactly("a", "b", "e", "f");

        commands.sortAndStore(k, "dest1", new SortArgs().alpha());
        commands.sortAndStore(key, "dest2");

        ListCommands<String, String> listCommands = ds.list(String.class, String.class);
        assertThat(listCommands.lrange("dest1", 0, -1)).containsExactly("a", "b", "e", "f");
        assertThat(listCommands.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void testSetWithTypeReference() {
        var sets = ds.set(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        assertThat(sets.sadd(key, List.of(person1, person2))).isEqualTo(1L);
        assertThat(sets.sadd(key, List.of(person1, person2))).isEqualTo(0);
        assertThat(sets.smembers(key)).isEqualTo(Set.of(List.of(person1, person2)));
        assertThat(sets.sadd(key, List.of(person2, person3), List.of(person4))).isEqualTo(2);
        assertThat(sets.smembers(key)).containsExactlyInAnyOrder(List.of(person1, person2), List.of(person2, person3),
                List.of(person4));
    }

}

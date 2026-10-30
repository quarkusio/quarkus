package io.quarkus.redis.lettuce.runtime.internal.hash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.offset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hash.HashScanCursor;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.vertx.core.json.Json;
import tools.jackson.core.type.TypeReference;

class LettuceHashCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveHashCommands<String, String, Person> reactiveHash;
    HashCommands<String, String, Person> blockingHash;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveHash = reactiveDs.hash(Person.class);
        blockingHash = blockingDs.hash(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveHash.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingHash.getDataSource());
    }

    @Test
    void simpleHset() {
        blockingHash.hset("my-hash", "field1", Person.person1);
        Person person = blockingHash.hget("my-hash", "field1");
        assertThat(person).isEqualTo(Person.person1);

        assertThat(blockingHash.hdel("my-hash", "field1")).isEqualTo(1);
        person = blockingHash.hget("my-hash", "field1");
        assertThat(person).isNull();
    }

    @Test
    void hsetWithTypeReference() {
        var h = blockingDs.hash(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        h.hset("my-hash", "l1", List.of(Person.person1, Person.person2));
        List<Person> person = h.hget("my-hash", "l1");
        assertThat(person).containsExactly(Person.person1, Person.person2);

        assertThat(h.hdel("my-hash", "l1")).isEqualTo(1);
        person = h.hget("my-hash", "l1");
        assertThat(person).isNull();
    }

    @Test
    void hsetWithTypeReferenceUsingMaps() {
        var h = blockingDs.hash(new TypeReference<Map<String, Person>>() {
            // Empty on purpose.
        });
        h.hset("my-hash", "l1", Map.of("a", Person.person1, "b", Person.person2));
        Map<String, Person> person = h.hget("my-hash", "l1");
        assertThat(person).containsOnly(entry("a", Person.person1), entry("b", Person.person2));

        assertThat(h.hdel("my-hash", "l1")).isEqualTo(1);
        person = h.hget("my-hash", "l1");
        assertThat(person).isNull();
    }

    @Test
    void hdel() {
        assertThat(blockingHash.hdel(key, "one")).isEqualTo(0);
        blockingHash.hset(key, "two", Person.person1);
        assertThat(blockingHash.hdel(key, "one")).isEqualTo(0);
        blockingHash.hset(key, "one", Person.person2);
        assertThat(blockingHash.hdel(key, "one")).isEqualTo(1);
        blockingHash.hset(key, "one", Person.person2);
        assertThat(blockingHash.hdel(key, "one", "two")).isEqualTo(2);
    }

    @Test
    void hexists() {
        assertThat(blockingHash.hexists(key, "one")).isFalse();
        blockingHash.hset(key, "two", Person.person2);
        assertThat(blockingHash.hexists(key, "one")).isFalse();
        blockingHash.hset(key, "one", Person.person1);
        assertThat(blockingHash.hexists(key, "one")).isTrue();
    }

    @Test
    void hget() {
        assertThat(blockingHash.hget(key, "one")).isNull();
        blockingHash.hset(key, "one", Person.person1);
        assertThat(blockingHash.hget(key, "one")).isEqualTo(Person.person1);
    }

    @Test
    void hgetall() {
        assertThat(blockingHash.hgetall(key).isEmpty()).isTrue();

        blockingHash.hset(key, "zero", Person.person0);
        blockingHash.hset(key, "one", Person.person1);
        blockingHash.hset(key, "two", Person.person2);

        Map<String, Person> map = blockingHash.hgetall(key);

        assertThat(map).hasSize(3);
        assertThat(map.keySet()).containsExactlyInAnyOrder("zero", "one", "two");
        assertThat(map.values()).containsExactlyInAnyOrder(Person.person0, Person.person1, Person.person2);

        assertThat(blockingHash.hgetall("missing")).isEmpty();
    }

    /**
     * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/28837">#28837</a>.
     */
    @Test
    void hgetallUsingIntegers() {
        var cmd = blockingDs.hash(Integer.class);
        String key = UUID.randomUUID().toString();
        assertThat(cmd.hgetall(key).isEmpty()).isTrue();

        cmd.hset(key, Map.of("a", 1, "b", 2, "c", 3));

        Map<String, Integer> map = cmd.hgetall(key);

        assertThat(map).hasSize(3);
    }

    @Test
    void hincrby() {
        assertThat(blockingHash.hincrby(key, "one", 1)).isEqualTo(1);
        assertThat(blockingHash.hincrby(key, "one", -2)).isEqualTo(-1);
    }

    @Test
    void hincrbyfloat() {
        assertThat(blockingHash.hincrbyfloat(key, "one", 1.0)).isEqualTo(1.0);
        assertThat(blockingHash.hincrbyfloat(key, "one", -2.0)).isEqualTo(-1.0);
        assertThat(blockingHash.hincrbyfloat(key, "one", 1.23)).isEqualTo(0.23, offset(0.001));
    }

    @Test
    void hkeys() {
        populate();
        List<String> keys = blockingHash.hkeys(key);
        assertThat(keys).hasSize(2);
        assertThat(keys).containsExactly("one", "two");
    }

    private void populate() {
        assertThat(blockingHash.hkeys(key)).isEqualTo(Collections.emptyList());
        blockingHash.hset(key, "one", Person.person1);
        blockingHash.hset(key, "two", Person.person2);
    }

    @Test
    void hlen() {
        assertThat(blockingHash.hlen(key)).isEqualTo(0);
        blockingHash.hset(key, "one", Person.person1);
        assertThat(blockingHash.hlen(key)).isEqualTo(1);
    }

    @Test
    void hstrlen() {
        assertThat(blockingHash.hstrlen(key, "one")).isEqualTo(0);
        blockingHash.hset(key, "one", Person.person1);
        assertThat(blockingHash.hstrlen(key, "one")).isEqualTo(Json.encode(Person.person1).length());
    }

    @Test
    void hmget() {
        populateForHmget();
        Map<String, Person> values = blockingHash.hmget(key, "one", "missing", "two");
        assertThat(values).hasSize(3);
        assertThat(values).containsExactly(entry("one", Person.person1), entry("missing", null),
                entry("two", Person.person2));
    }

    private void populateForHmget() {
        assertThat(blockingHash.hmget(key, "one", "two")).allSatisfy((s, p) -> assertThat(p).isNull());
        blockingHash.hset(key, "one", Person.person1);
        blockingHash.hset(key, "two", Person.person2);
    }

    @Test
    void hmset() {
        blockingHash.hmset(key, Map.of("one", Person.person1, "two", Person.person2));
        assertThat(blockingHash.hmget(key, "one", "two")).containsExactly(entry("one", Person.person1),
                entry("two", Person.person2));
    }

    @Test
    void hmsetWithNulls() {
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("one", null);
        blockingHash.hmset(key, map);
        assertThat(blockingHash.hmget(key, "one")).containsExactly(entry("one", null));

        map.put("one", Person.person1);
        blockingHash.hmset(key, map);
        assertThat(blockingHash.hmget(key, "one")).containsExactly(entry("one", Person.person1));
    }

    @Test
    void hrandfield() {
        blockingHash.hset(key, Map.of("one", Person.person1, "two", Person.person2, "three", Person.person3));

        assertThat(blockingHash.hrandfield(key)).isIn("one", "two", "three");
        assertThat(blockingHash.hrandfield(key, 2)).hasSize(2).containsAnyOf("one", "two", "three");
    }

    @Test
    void hrandfieldWithValues() {
        Map<String, Person> map = Map.of("one", Person.person1, "two", Person.person2, "three", Person.person3);
        blockingHash.hset(key, map);

        assertThat(blockingHash.hrandfieldWithValues(key, 1))
                .anySatisfy((s, p) -> assertThat(map.get(s)).isEqualTo(p));
        assertThat(blockingHash.hrandfieldWithValues(key, 2)).hasSize(2)
                .allSatisfy((s, p) -> assertThat(map.get(s)).isEqualTo(p));

        assertThat(blockingHash.hrandfieldWithValues(key, -20)).isNotEmpty();
        assertThat(blockingHash.hrandfieldWithValues(key, 3)).containsExactlyInAnyOrderEntriesOf(map);

        assertThat(blockingHash.hrandfieldWithValues("missing", 3)).isEmpty();
    }

    @Test
    void hset() {
        assertThat(blockingHash.hset(key, "one", Person.person1)).isTrue();
        assertThat(blockingHash.hset(key, "one", Person.person1)).isFalse();
    }

    @Test
    void hsetMap() {
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("two", Person.person2);
        map.put("three", Person.person0);
        assertThat(blockingHash.hset(key, map)).isEqualTo(2);

        map.put("two", Person.person2);
        assertThat(blockingHash.hset(key, map)).isEqualTo(0);
        assertThat(blockingHash.hget(key, "two")).isEqualTo(Person.person2);
    }

    @Test
    void hsetnx() {
        blockingHash.hset(key, "one", Person.person1);
        assertThat(blockingHash.hsetnx(key, "one", Person.person2)).isFalse();
        assertThat(blockingHash.hget(key, "one")).isEqualTo(Person.person1);
    }

    @Test
    void hvals() {
        assertThat(blockingHash.hvals(key)).isEqualTo(List.of());
        blockingHash.hset(key, "one", Person.person1);
        blockingHash.hset(key, "two", Person.person2);
        List<Person> values = blockingHash.hvals(key);
        assertThat(values).hasSize(2)
                .containsExactly(Person.person1, Person.person2);
    }

    @Test
    void hscan() {
        blockingHash.hset(key, "one", Person.person0);
        HashScanCursor<String, Person> cursor = blockingHash.hscan(key);

        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(next).containsExactly(entry("one", Person.person0));
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void hscanEmpty() {
        HashScanCursor<String, Person> cursor = blockingHash.hscan(key);

        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(cursor.hasNext()).isFalse();
        assertThat(next).isEmpty();
    }

    @Test
    void hscanAsIteratorEmpty() {
        HashScanCursor<String, Person> cursor = blockingHash.hscan(key);
        Iterable<Map.Entry<String, Person>> iterable = cursor.toIterable();

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Person> entry : iterable) {
            keys.add(entry.getKey());
        }
        assertThat(keys).isEmpty();
    }

    @Test
    void hscanWithArgs() {
        blockingHash.hset(key, "one", Person.person0);
        blockingHash.hset(key, "two", Person.person1);
        blockingHash.hset(key, "three", Person.person2);
        HashScanCursor<String, Person> cursor = blockingHash.hscan(key, new ScanArgs().count(3));

        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(next).containsExactly(entry("one", Person.person0), entry("two", Person.person1),
                entry("three", Person.person2));
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void hscanMultiple() {
        Map<String, Person> expect = new LinkedHashMap<>();
        Map<String, Person> check = new LinkedHashMap<>();
        populateManyEntries(expect);

        HashScanCursor<String, Person> cursor = blockingHash.hscan(key, new ScanArgs().count(5));
        while (cursor.hasNext()) {
            check.putAll(cursor.next());
        }

        assertThat(check).isEqualTo(expect);
    }

    @Test
    void hscanIterator() {
        Map<String, Person> expect = new LinkedHashMap<>();
        Map<String, Person> check = new LinkedHashMap<>();
        populateManyEntries(expect);

        HashScanCursor<String, Person> cursor = blockingHash.hscan(key, new ScanArgs().count(5));
        Iterable<Map.Entry<String, Person>> entries = cursor.toIterable();
        for (Map.Entry<String, Person> entry : entries) {
            check.put(entry.getKey(), entry.getValue());
        }

        assertThat(cursor.hasNext()).isFalse();
        assertThat(check).isEqualTo(expect);
    }

    @Test
    void hscanMatch() {
        Map<String, Person> expect = new LinkedHashMap<>();
        Map<String, Person> check = new HashMap<>();

        populateManyEntries(expect);

        HashScanCursor<String, Person> cursor = blockingHash.hscan(key, new ScanArgs().match("f1*"));
        while (cursor.hasNext()) {
            check.putAll(cursor.next());
        }

        assertThat(check).hasSize(11);
    }

    private void populateManyEntries(Map<String, Person> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put("f" + i, new Person("name" + i, ""));
        }
        blockingHash.hset(key, expect);
    }

    /**
     * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/42131">#42131</a>.
     */
    @Test
    void testInvalidHashMGet() {
        HashCommands<String, String, String> cmd = blockingDs.hash(String.class, String.class, String.class);
        // Key must not be null
        assertThatThrownBy(() -> cmd.hmget(null, "a", "b")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
        // Fields must not be empty
        assertThatThrownBy(() -> cmd.hmget("key")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fields");

        // Fields must not contain `null`
        assertThatThrownBy(() -> cmd.hmget("key", null, "b")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fields");
        assertThatThrownBy(() -> cmd.hmget("key", "a", null, "b")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fields");
    }

    @Test
    void hscanReactiveToMulti() {
        Map<String, Person> expect = new LinkedHashMap<>();
        populateManyEntries(expect);

        ReactiveHashScanCursor<String, Person> cursor = reactiveHash.hscan(key, new ScanArgs().count(5));
        List<Map.Entry<String, Person>> entries = cursor.toMulti().collect().asList().await().atMost(TIMEOUT);

        Map<String, Person> check = new LinkedHashMap<>();
        entries.forEach(e -> check.put(e.getKey(), e.getValue()));
        assertThat(check).isEqualTo(expect);
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void hscanCursorId() {
        blockingHash.hset(key, "one", Person.person1);

        ReactiveHashScanCursor<String, Person> cursor = reactiveHash.hscan(key);
        assertThat(cursor.cursorId()).isEqualTo(0L);

        cursor.next().await().atMost(TIMEOUT);

        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0L);
    }

    @Test
    void apiReturnsImmutableCollections() {
        blockingHash.hset(key, Map.of("f1", Person.person1, "f2", Person.person2));
        assertImmutable(blockingHash.hgetall(key));
        assertImmutable(blockingHash.hgetall("missing"));
        assertImmutable(blockingHash.hkeys(key));
        assertImmutable(blockingHash.hvals(key));
        assertImmutable(blockingHash.hmget(key, "f1", "nope"));
        assertImmutable(blockingHash.hrandfield(key, 2));
        assertImmutable(blockingHash.hrandfieldWithValues(key, 2));
        assertImmutable(blockingHash.hrandfieldWithValues("missing", 2));
        assertImmutable(blockingHash.hscan(key).next());
        assertImmutable(blockingHash.hscan("missing", new ScanArgs().count(10)).next());
    }

}

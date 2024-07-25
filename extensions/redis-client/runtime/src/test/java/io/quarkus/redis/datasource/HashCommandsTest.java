package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.offset;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hash.HashScanCursor;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.core.json.Json;

public class HashCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;
    private HashCommands<String, String, Person> hash;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        hash = ds.hash(Person.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(hash.getDataSource());
    }

    @Test
    void simpleHset() {
        hash.hset("my-hash", "field1", Person.person1);
        Person person = hash.hget("my-hash", "field1");
        assertThat(person).isEqualTo(Person.person1);

        assertThat(hash.hdel("my-hash", "field1")).isEqualTo(1);
        person = hash.hget("my-hash", "field1");
        assertThat(person).isNull();
    }

    @Test
    void HSetWithTypeReference() {
        var h = ds.hash(new TypeReference<List<Person>>() {
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
    void HSetWithTypeReferenceUsingMaps() {
        var h = ds.hash(new TypeReference<Map<String, Person>>() {
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
        assertThat(hash.hdel(key, "one")).isEqualTo(0);
        hash.hset(key, "two", Person.person1);
        assertThat(hash.hdel(key, "one")).isEqualTo(0);
        hash.hset(key, "one", Person.person2);
        assertThat(hash.hdel(key, "one")).isEqualTo(1);
        hash.hset(key, "one", Person.person2);
        assertThat(hash.hdel(key, "one", "two")).isEqualTo(2);
    }

    @Test
    void hexists() {
        assertThat(hash.hexists(key, "one")).isFalse();
        hash.hset(key, "two", Person.person2);
        assertThat(hash.hexists(key, "one")).isFalse();
        hash.hset(key, "one", Person.person1);
        assertThat(hash.hexists(key, "one")).isTrue();
    }

    @Test
    void hget() {
        assertThat(hash.hget(key, "one")).isNull();
        hash.hset(key, "one", Person.person1);
        assertThat(hash.hget(key, "one")).isEqualTo(Person.person1);
    }

    @Test
    public void hgetall() {
        assertThat(hash.hgetall(key).isEmpty()).isTrue();

        hash.hset(key, "zero", Person.person0);
        hash.hset(key, "one", Person.person1);
        hash.hset(key, "two", Person.person2);

        Map<String, Person> map = hash.hgetall(key);

        assertThat(map).hasSize(3);
        assertThat(map.keySet()).containsExactlyInAnyOrder("zero", "one", "two");
        assertThat(map.values()).containsExactlyInAnyOrder(Person.person0, Person.person1, Person.person2);

        assertThat(hash.hgetall("missing")).isEmpty();
    }

    /**
     * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/28837">#28837</a>.
     */
    @Test
    public void hgetallUsingIntegers() {
        var cmd = ds.hash(Integer.class);
        String key = UUID.randomUUID().toString();
        assertThat(cmd.hgetall(key).isEmpty()).isTrue();

        cmd.hset(key, Map.of("a", 1, "b", 2, "c", 3));

        Map<String, Integer> map = cmd.hgetall(key);

        assertThat(map).hasSize(3);
    }

    @Test
    void hincrby() {
        assertThat(hash.hincrby(key, "one", 1)).isEqualTo(1);
        assertThat(hash.hincrby(key, "one", -2)).isEqualTo(-1);
    }

    @Test
    void hincrbyfloat() {
        assertThat(hash.hincrbyfloat(key, "one", 1.0)).isEqualTo(1.0);
        assertThat(hash.hincrbyfloat(key, "one", -2.0)).isEqualTo(-1.0);
        assertThat(hash.hincrbyfloat(key, "one", 1.23)).isEqualTo(0.23, offset(0.001));
    }

    @Test
    void hkeys() {
        populate();
        List<String> keys = hash.hkeys(key);
        assertThat(keys).hasSize(2);
        assertThat(keys).containsExactly("one", "two");
    }

    private void populate() {
        assertThat(hash.hkeys(key)).isEqualTo(Collections.emptyList());
        hash.hset(key, "one", Person.person1);
        hash.hset(key, "two", Person.person2);
    }

    @Test
    void hlen() {
        assertThat(hash.hlen(key)).isEqualTo(0);
        hash.hset(key, "one", Person.person1);
        assertThat(hash.hlen(key)).isEqualTo(1);
    }

    @Test
    void hstrlen() {
        assertThat(hash.hstrlen(key, "one")).isEqualTo(0);
        hash.hset(key, "one", Person.person1);
        assertThat(hash.hstrlen(key, "one")).isEqualTo(Json.encode(Person.person1).length());
    }

    @Test
    void hmget() {
        populateForHmget();
        Map<String, Person> values = hash.hmget(key, "one", "missing", "two");
        assertThat(values).hasSize(3);
        assertThat(values).containsExactly(entry("one", Person.person1), entry("missing", null), entry("two", Person.person2));
    }

    private void populateForHmget() {
        assertThat(hash.hmget(key, "one", "two")).allSatisfy((s, p) -> assertThat(p).isNull());
        hash.hset(key, "one", Person.person1);
        hash.hset(key, "two", Person.person2);
    }

    @Test
    void hmset() {
        hash.hmset(key, Map.of("one", Person.person1, "two", Person.person2));
        assertThat(hash.hmget(key, "one", "two")).containsExactly(entry("one", Person.person1), entry("two", Person.person2));
    }

    @Test
    void hmsetWithNulls() {
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("one", null);
        hash.hmset(key, map);
        assertThat(hash.hmget(key, "one")).containsExactly(entry("one", null));

        map.put("one", Person.person1);
        hash.hmset(key, map);
        assertThat(hash.hmget(key, "one")).containsExactly(entry("one", Person.person1));
    }

    @Test
    @RequiresRedis7OrHigher
    void hrandfield() {
        hash.hset(key, Map.of("one", Person.person1, "two", Person.person2, "three", Person.person3));

        assertThat(hash.hrandfield(key)).isIn("one", "two", "three");
        assertThat(hash.hrandfield(key, 2)).hasSize(2).containsAnyOf("one", "two", "three");
    }

    @Test
    @RequiresRedis7OrHigher
    void hrandfieldWithValues() {
        Map<String, Person> map = Map.of("one", Person.person1, "two", Person.person2, "three", Person.person3);
        hash.hset(key, map);

        assertThat(hash.hrandfieldWithValues(key, 1))
                .anySatisfy((s, p) -> assertThat(map.get(s)).isEqualTo(p));
        assertThat(hash.hrandfieldWithValues(key, 2)).hasSize(2)
                .allSatisfy((s, p) -> assertThat(map.get(s)).isEqualTo(p));

        assertThat(hash.hrandfieldWithValues(key, -20)).isNotEmpty();
        assertThat(hash.hrandfieldWithValues(key, 3)).containsExactlyInAnyOrderEntriesOf(map);

        assertThat(hash.hrandfieldWithValues("missing", 3)).isEmpty();
    }

    @Test
    void hset() {
        assertThat(hash.hset(key, "one", Person.person1)).isTrue();
        assertThat(hash.hset(key, "one", Person.person1)).isFalse();
    }

    @Test
    void hsetMap() {
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("two", Person.person2);
        map.put("three", Person.person0);
        assertThat(hash.hset(key, map)).isEqualTo(2);

        map.put("two", Person.person2);
        assertThat(hash.hset(key, map)).isEqualTo(0);
        assertThat(hash.hget(key, "two")).isEqualTo(Person.person2);
    }

    @Test
    void hsetnx() {
        hash.hset(key, "one", Person.person1);
        assertThat(hash.hsetnx(key, "one", Person.person2)).isFalse();
        assertThat(hash.hget(key, "one")).isEqualTo(Person.person1);
    }

    @Test
    void hvals() {
        assertThat(hash.hvals(key)).isEqualTo(List.of());
        hash.hset(key, "one", Person.person1);
        hash.hset(key, "two", Person.person2);
        List<Person> values = hash.hvals(key);
        assertThat(values).hasSize(2)
                .containsExactly(Person.person1, Person.person2);
    }

    @Test
    void hscan() {
        hash.hset(key, "one", Person.person0);
        HashScanCursor<String, Person> cursor = hash.hscan(key);

        assertThat(cursor.cursorId()).isEqualTo(-1L);
        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(next).containsExactly(entry("one", Person.person0));
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void hscanEmpty() {
        HashScanCursor<String, Person> cursor = hash.hscan(key);

        assertThat(cursor.cursorId()).isEqualTo(-1L);
        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(next).isEmpty();
    }

    @Test
    void hscanAsIteratorEmpty() {
        HashScanCursor<String, Person> cursor = hash.hscan(key);
        Iterable<Map.Entry<String, Person>> iterable = cursor.toIterable();

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Person> entry : iterable) {
            keys.add(entry.getKey());
        }
        assertThat(keys).isEmpty();
    }

    @Test
    void hscanWithArgs() {
        hash.hset(key, "one", Person.person0);
        hash.hset(key, "two", Person.person1);
        hash.hset(key, "three", Person.person2);
        HashScanCursor<String, Person> cursor = hash.hscan(key, new ScanArgs().count(3));

        assertThat(cursor.cursorId()).isEqualTo(-1L);
        assertThat(cursor.hasNext()).isTrue();
        Map<String, Person> next = cursor.next();

        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(next).containsExactly(entry("one", Person.person0), entry("two", Person.person1),
                entry("three", Person.person2));
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void hscanMultiple() {
        Map<String, Person> expect = new LinkedHashMap<>();
        Map<String, Person> check = new LinkedHashMap<>();
        populateManyEntries(expect);

        HashScanCursor<String, Person> cursor = hash.hscan(key, new ScanArgs().count(5));
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

        HashScanCursor<String, Person> cursor = hash.hscan(key, new ScanArgs().count(5));
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

        HashScanCursor<String, Person> cursor = hash.hscan(key, new ScanArgs().match("f1*"));
        while (cursor.hasNext()) {
            check.putAll(cursor.next());
        }

        assertThat(check).hasSize(11);
    }

    void populateManyEntries(Map<String, Person> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put("f" + i, new Person("name" + i, ""));
        }
        hash.hset(key, expect);
    }

    void populateManyEntries(HashCommands<String, String, String> hash, Map<String, String> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put("f" + i, "hello" + i);
        }
        hash.hset(key, expect);
    }

    /**
     * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/42131">#42131</a>.
     */
    @Test
    void testInvalidHashMGet() {
        HashCommands<String, String, String> cmd = ds.hash(String.class, String.class, String.class);
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

}

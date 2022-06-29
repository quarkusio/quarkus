package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.api.Cursor;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.api.keys.CopyArgs;
import io.quarkus.redis.datasource.api.keys.ExpireArgs;
import io.quarkus.redis.datasource.api.keys.KeyCommands;
import io.quarkus.redis.datasource.api.keys.KeyScanArgs;
import io.quarkus.redis.datasource.api.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.api.keys.RedisValueType;
import io.quarkus.redis.datasource.api.list.ListCommands;
import io.quarkus.redis.datasource.api.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.api.string.StringCommands;
import io.quarkus.redis.datasource.impl.BlockingRedisDataSourceImpl;

public class KeyCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-generic";
    private KeyCommands<String> keys;
    private StringCommands<String, Person> strings;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(redis, api, Duration.ofSeconds(1));

        strings = ds.string(Person.class);
        keys = ds.key();
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void del() {
        strings.set(key, Person.person7);
        assertThat((long) keys.del(key)).isEqualTo(1);
        strings.set(key + "1", Person.person7);
        strings.set(key + "2", Person.person7);

        assertThat(keys.del(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    void unlink() {
        strings.set(key, Person.person7);
        assertThat((long) keys.unlink(key)).isEqualTo(1);
        strings.set(key + "1", Person.person7);
        strings.set(key + "2", Person.person7);
        assertThat(keys.unlink(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    void copy() {
        strings.set(key, Person.person7);
        assertThat(keys.copy(key, key + "2")).isTrue();
        assertThat(keys.copy("unknown", key + "2")).isFalse();
        assertThat(strings.get(key + "2")).isEqualTo(Person.person7);
    }

    @Test
    void copyWithReplace() {
        strings.set(key, Person.person7);
        strings.set(key + 2, Person.person1);
        assertThat(keys.copy(key, key + "2", new CopyArgs().replace(true))).isTrue();
        assertThat(strings.get(key + "2")).isEqualTo(Person.person7);
    }

    @Test
    void copyWithDestinationDb() {
        ds.withConnection(connection -> {
            connection.string(String.class, Person.class).set(key, Person.person7);
            connection.key(String.class).copy(key, key, new CopyArgs().destinationDb(2));
            connection.select(2);
            assertThat(connection.string(String.class, Person.class).get(key)).isEqualTo(Person.person7);
        });
    }

    @Test
    void dump() {
        assertThat(keys.dump("invalid")).isNull();
        strings.set(key, Person.person7);
        assertThat(keys.dump(key).length() > 0).isTrue();
    }

    @Test
    void exists() {
        assertThat(keys.exists(key)).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.exists(key)).isTrue();
    }

    @Test
    void existsVariadic() {
        assertThat(keys.exists(key, "key2", "key3")).isEqualTo(0);
        strings.set(key, Person.person7);
        strings.set("key2", Person.person7);
        assertThat(keys.exists(key, "key2", "key3")).isEqualTo(2);
    }

    @Test
    void expire() {
        assertThat(keys.expire(key, 10)).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.expire(key, 10)).isTrue();
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        assertThat(keys.expire(key, Duration.ofSeconds(20))).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireWithArgs() {
        assertThat(keys.expire(key, 10, new ExpireArgs().xx())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.expire(key, 10, new ExpireArgs().nx())).isTrue();
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        assertThat(keys.expire(key, Duration.ofSeconds(20), new ExpireArgs().gt())).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireat() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(keys.expireat(key, expiration.toInstant().toEpochMilli())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.expireat(key, expiration.toInstant())).isTrue();

        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(8);

        assertThat(keys.expireat(key, Instant.now().plusSeconds(15))).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireatWithArgs() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(keys.expireat(key, expiration.toInstant().getEpochSecond(), new ExpireArgs().xx())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.expireat(key, expiration.toInstant(), new ExpireArgs().nx())).isTrue();

        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(8);

        Instant timestamp = Instant.now().plusSeconds(15);
        assertThat(keys.expireat(key, timestamp, new ExpireArgs().gt())).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);

        assertThat(keys.expiretime(key)).isEqualTo(timestamp.getEpochSecond());
    }

    @Test
    void keys() {
        assertThat(keys.keys("*")).isEqualTo(List.of());
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("one", Person.person1);
        map.put("two", Person.person2);
        map.put("three", Person.person3);
        strings.mset(map);
        List<String> k = keys.keys("???");
        assertThat(k).hasSize(2);
        assertThat(k.contains("one")).isTrue();
        assertThat(k.contains("two")).isTrue();
    }

    @Test
    public void move() {
        ds.withConnection(connection -> {
            StringCommands<String, Person> commands = connection.string(String.class, Person.class);
            commands.set("foo", Person.person3);
            commands.set(key, Person.person7);
            assertThat(connection.key(String.class).move(key, 1)).isTrue();
            assertThat(commands.get(key)).isNull();
            connection.select(1);
            assertThat(commands.get(key)).isEqualTo(Person.person7);
        });

    }

    @Test
    void persist() {
        assertThat(keys.persist(key)).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.persist(key)).isFalse();
        keys.expire(key, 10);
        assertThat(keys.persist(key)).isTrue();
    }

    @Test
    void pexpire() {
        assertThat(keys.pexpire(key, 5000)).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.pexpire(key, 5000)).isTrue();
        assertThat(keys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);

        keys.pexpire(key, Duration.ofSeconds(20));
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireWithArgs() {
        assertThat(keys.pexpire(key, 5000, new ExpireArgs().xx())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.pexpire(key, 5000, new ExpireArgs().nx())).isTrue();
        assertThat(keys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);

        keys.pexpire(key, Duration.ofSeconds(20), new ExpireArgs().gt());
        assertThat(keys.ttl(key)).isBetween(10L, 20L);

        assertThat(keys.pexpiretime(key)).isBetween(System.currentTimeMillis() - 10000L, System.currentTimeMillis() + 30000L);
    }

    @Test
    void pexpireWithDuration() {
        assertThat(keys.pexpire(key, Duration.ofSeconds(5))).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.pexpire(key, Duration.ofSeconds(1))).isTrue();
        assertThat(keys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(1000);

        assertThat(keys.pexpire(key, Duration.ofSeconds(20), new ExpireArgs().gt())).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireat() {
        Instant expiration = new Date(System.currentTimeMillis() + 5000).toInstant();
        assertThat(keys.pexpireat(key, expiration.getEpochSecond())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.pexpireat(key, expiration)).isTrue();
        assertThat(keys.pttl(key)).isGreaterThan(0);

        assertThat(keys.pexpireat(key, Instant.now().plusSeconds(15))).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireatWithArgs() {
        Instant expiration = new Date(System.currentTimeMillis() + 5000).toInstant();
        assertThat(keys.pexpireat(key, expiration.getEpochSecond(), new ExpireArgs().xx())).isFalse();
        strings.set(key, Person.person7);
        assertThat(keys.pexpireat(key, expiration, new ExpireArgs().nx())).isTrue();
        assertThat(keys.pttl(key)).isGreaterThan(0);

        assertThat(keys.pexpireat(key, Instant.now().plusSeconds(15), new ExpireArgs().gt())).isTrue();
        assertThat(keys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pttl() {
        assertThatThrownBy(() -> keys.pttl(key)).isInstanceOf(RedisKeyNotFoundException.class);
        strings.set(key, Person.person7);
        assertThat(keys.pttl(key)).isEqualTo(-1);
        keys.pexpire(key, 5000);
        assertThat(keys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void randomkey() {
        assertThat(keys.randomkey()).isNull();
        strings.set(key, Person.person7);
        assertThat(keys.randomkey()).isEqualTo(key);
    }

    @Test
    void rename() {
        strings.set(key, Person.person7);

        keys.rename(key, key + "X");
        assertThat(strings.get(key)).isNull();
        assertThat(strings.get(key + "X")).isEqualTo(Person.person7);
        strings.set(key, Person.person4);
        keys.rename(key + "X", key);
        assertThat(strings.get(key)).isEqualTo(Person.person7);
    }

    @Test
    void renameNonexistentKey() {
        assertThatThrownBy(() -> keys.rename(key, key + "X")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void renamenx() {
        strings.set(key, Person.person7);
        assertThat(keys.renamenx(key, key + "X")).isTrue();
        assertThat(strings.get(key + "X")).isEqualTo(Person.person7);
        strings.set(key, Person.person7);
        assertThat(keys.renamenx(key + "X", key)).isFalse();
    }

    @Test
    void renamenxNonexistentKey() {
        assertThatThrownBy(() -> keys.renamenx(key, key + "X")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void touch() {
        assertThat((long) keys.touch(key)).isEqualTo(0);
        strings.set(key, Person.person7);
        assertThat((long) keys.touch(key, "key2")).isEqualTo(1);
    }

    @Test
    void ttl() {
        assertThatThrownBy(() -> keys.pttl(key)).isInstanceOf(RedisKeyNotFoundException.class);
        strings.set(key, Person.person7);
        assertThat(keys.ttl(key)).isEqualTo(-1);
        keys.expire(key, 10);
        assertThat(keys.ttl(key)).isEqualTo(10);
    }

    @Test
    void type() {
        assertThat(keys.type(key)).isEqualTo(RedisValueType.NONE);

        strings.set(key, Person.person7);
        assertThat(keys.type(key)).isEqualTo(RedisValueType.STRING);

        ds.hash(String.class, String.class, Person.class).hset(key + "H", "p3", Person.person3);
        assertThat(keys.type(key + "H")).isEqualTo(RedisValueType.HASH);

        ListCommands<String, String> lists = ds.list(String.class);
        lists.lpush(key + "L", "1");
        assertThat(keys.type(key + "L")).isEqualTo(RedisValueType.LIST);

        ds.set(String.class, Person.class).sadd(key + "S", Person.person4);
        assertThat(keys.type(key + "S")).isEqualTo(RedisValueType.SET);

        SortedSetCommands<String, String> ss = ds.sortedSet(String.class);
        ss.zadd(key + "Z", 1, "1");
        assertThat(keys.type(key + "Z")).isEqualTo(RedisValueType.ZSET);
    }

    @Test
    void scan() {
        strings.set(key, Person.person7);
        Cursor<Set<String>> cursor = keys.scan();
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.next()).containsExactly(key);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0);
    }

    @Test
    void scanWithArgs() {
        strings.set(key, Person.person7);
        Cursor<Set<String>> cursor = keys.scan(new KeyScanArgs().count(10));
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.next()).containsExactly(key);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0);
    }

    @Test
    void scanWithType() {
        strings.set("key1", Person.person7);
        ds.list(Person.class).lpush("key2", Person.person7);

        Cursor<Set<String>> cursor = keys.scan(new KeyScanArgs().type(RedisValueType.STRING));
        assertThat(cursor.next()).containsExactly("key1");

        cursor = keys.scan(new KeyScanArgs().type(RedisValueType.LIST));
        assertThat(cursor.next()).containsExactly("key2");
    }

    @Test
    void scanMultiple() {
        Set<String> expect = new HashSet<>();
        populateMany(expect);

        Cursor<Set<String>> cursor = keys.scan(new KeyScanArgs().count(12));

        assertThat(cursor.cursorId()).isNotEqualTo(0);
        assertThat(cursor.hasNext()).isTrue();

        Set<String> check = new HashSet<>(cursor.next());

        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        assertThat(check).isEqualTo(expect);
        assertThat(check).hasSize(100);
    }

    @Test
    void scanMatch() {
        Set<String> expect = new HashSet<>();
        populateMany(expect);
        Cursor<Set<String>> cursor = keys.scan(new KeyScanArgs().count(200).match(key + "*"));
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.next()).hasSize(expect.size());
        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.cursorId()).isEqualTo(0);
    }

    void populateMany(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            strings.set(key + i, new Person("a", "b" + i));
            expect.add(key + i);
        }
    }

}

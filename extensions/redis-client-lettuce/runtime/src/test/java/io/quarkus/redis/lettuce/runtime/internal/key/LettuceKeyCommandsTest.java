package io.quarkus.redis.lettuce.runtime.internal.key;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceKeyCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveKeyCommands<String> reactiveKeys;
    KeyCommands<String> blockingKeys;
    ValueCommands<String, Person> blockingValues;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveKeys = reactiveDs.key(String.class);
        blockingKeys = blockingDs.key(String.class);
        blockingValues = blockingDs.value(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveKeys.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingKeys.getDataSource());
    }

    @Test
    void del() {
        blockingValues.set(key, Person.person7);
        assertThat((long) blockingKeys.del(key)).isEqualTo(1);
        blockingValues.set(key + "1", Person.person7);
        blockingValues.set(key + "2", Person.person7);

        assertThat(blockingKeys.del(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    void unlink() {
        blockingValues.set(key, Person.person7);
        assertThat((long) blockingKeys.unlink(key)).isEqualTo(1);
        blockingValues.set(key + "1", Person.person7);
        blockingValues.set(key + "2", Person.person7);
        assertThat(blockingKeys.unlink(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    void copy() {
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.copy(key, key + "2")).isTrue();
        assertThat(blockingKeys.copy("unknown", key + "2")).isFalse();
        assertThat(blockingValues.get(key + "2")).isEqualTo(Person.person7);
    }

    @Test
    void copyWithReplace() {
        blockingValues.set(key, Person.person7);
        blockingValues.set(key + 2, Person.person1);
        assertThat(blockingKeys.copy(key, key + "2", new CopyArgs().replace(true))).isTrue();
        assertThat(blockingValues.get(key + "2")).isEqualTo(Person.person7);
    }

    @Test
    void copyWithDestinationDb() {
        blockingDs.withConnection(connection -> {
            connection.value(String.class, Person.class).set(key, Person.person7);
            connection.key(String.class).copy(key, key, new CopyArgs().destinationDb(2));
            connection.select(2);
            assertThat(connection.value(String.class, Person.class).get(key)).isEqualTo(Person.person7);
        });
    }

    @Test
    void dump() {
        assertThat(blockingKeys.dump("invalid")).isNull();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.dump(key)).isNotEmpty();
    }

    @Test
    void exists() {
        assertThat(blockingKeys.exists(key)).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.exists(key)).isTrue();
    }

    @Test
    void existsVariadic() {
        assertThat(blockingKeys.exists(key, "key2", "key3")).isEqualTo(0);
        blockingValues.set(key, Person.person7);
        blockingValues.set("key2", Person.person7);
        assertThat(blockingKeys.exists(key, "key2", "key3")).isEqualTo(2);
    }

    @Test
    void expire() {
        assertThat(blockingKeys.expire(key, 10)).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.expire(key, 10)).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(5L, 10L);

        assertThat(blockingKeys.expire(key, Duration.ofSeconds(20))).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireWithArgs() {
        assertThat(blockingKeys.expire(key, 10, new ExpireArgs().xx())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.expire(key, 10, new ExpireArgs().nx())).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(5L, 10L);

        assertThat(blockingKeys.expire(key, Duration.ofSeconds(20), new ExpireArgs().gt())).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireat() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(blockingKeys.expireat(key, expiration.toInstant().toEpochMilli())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.expireat(key, expiration.toInstant())).isTrue();

        assertThat(blockingKeys.ttl(key)).isGreaterThanOrEqualTo(8);

        assertThat(blockingKeys.expireat(key, Instant.now().plusSeconds(15))).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void expireatWithArgs() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(blockingKeys.expireat(key, expiration.toInstant().getEpochSecond(), new ExpireArgs().xx())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.expireat(key, expiration.toInstant(), new ExpireArgs().nx())).isTrue();

        assertThat(blockingKeys.ttl(key)).isGreaterThanOrEqualTo(8);

        Instant timestamp = Instant.now().plusSeconds(15);
        assertThat(blockingKeys.expireat(key, timestamp, new ExpireArgs().gt())).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);

        assertThat(blockingKeys.expiretime(key)).isEqualTo(timestamp.getEpochSecond());
    }

    @Test
    void keys() {
        assertThat(blockingKeys.keys("*")).isEqualTo(List.of());
        Map<String, Person> map = new LinkedHashMap<>();
        map.put("one", Person.person1);
        map.put("two", Person.person2);
        map.put("three", Person.person3);
        blockingValues.mset(map);
        List<String> k = blockingKeys.keys("???");
        assertThat(k).hasSize(2);
        assertThat(k).contains("one", "two");
    }

    @Test
    void keysWithTypeReferences() {
        var v = blockingDs.value(new TypeReference<List<String>>() {
            // Empty on purpose
        }, new TypeReference<Person>() {
            // Empty on purpose
        });
        var k = blockingDs.key(new TypeReference<List<String>>() {
            // Empty on purpose
        });

        assertThat(k.keys("*")).isEqualTo(List.of());
        Map<List<String>, Person> map = new LinkedHashMap<>();
        map.put(List.of("one"), Person.person1);
        map.put(List.of("two"), Person.person2);
        map.put(List.of("three"), Person.person3);
        v.mset(map);
        var l = k.keys("*o*");
        assertThat(l).hasSize(2);
        assertThat(l).contains(List.of("one"), List.of("two"));
        assertThat(l).doesNotContain(List.of("three"));
    }

    @Test
    void move() {
        blockingDs.withConnection(connection -> {
            ValueCommands<String, Person> commands = connection.value(String.class, Person.class);
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
        assertThat(blockingKeys.persist(key)).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.persist(key)).isFalse();
        blockingKeys.expire(key, 10);
        assertThat(blockingKeys.persist(key)).isTrue();
    }

    @Test
    void pexpire() {
        assertThat(blockingKeys.pexpire(key, 5000)).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pexpire(key, 5000)).isTrue();
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);

        blockingKeys.pexpire(key, Duration.ofSeconds(20));
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireWithArgs() {
        assertThat(blockingKeys.pexpire(key, 5000, new ExpireArgs().xx())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pexpire(key, 5000, new ExpireArgs().nx())).isTrue();
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);

        blockingKeys.pexpire(key, Duration.ofSeconds(20), new ExpireArgs().gt());
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);

        assertThat(blockingKeys.pexpiretime(key)).isBetween(System.currentTimeMillis() - 10000L,
                System.currentTimeMillis() + 30000L);
    }

    @Test
    void pexpireWithDuration() {
        assertThat(blockingKeys.pexpire(key, Duration.ofSeconds(5))).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pexpire(key, Duration.ofSeconds(1))).isTrue();
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(1000);

        assertThat(blockingKeys.pexpire(key, Duration.ofSeconds(20), new ExpireArgs().gt())).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireat() {
        Instant expiration = new Date(System.currentTimeMillis() + 5000).toInstant();
        assertThat(blockingKeys.pexpireat(key, expiration.getEpochSecond())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pexpireat(key, expiration)).isTrue();
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0);

        assertThat(blockingKeys.pexpireat(key, Instant.now().plusSeconds(15))).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pexpireatWithArgs() {
        Instant expiration = new Date(System.currentTimeMillis() + 5000).toInstant();
        assertThat(blockingKeys.pexpireat(key, expiration.getEpochSecond(), new ExpireArgs().xx())).isFalse();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pexpireat(key, expiration, new ExpireArgs().nx())).isTrue();
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0);

        assertThat(blockingKeys.pexpireat(key, Instant.now().plusSeconds(15), new ExpireArgs().gt())).isTrue();
        assertThat(blockingKeys.ttl(key)).isBetween(10L, 20L);
    }

    @Test
    void pttl() {
        assertThatThrownBy(() -> blockingKeys.pttl(key)).isInstanceOf(RedisKeyNotFoundException.class);
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.pttl(key)).isEqualTo(-1);
        blockingKeys.pexpire(key, 5000);
        assertThat(blockingKeys.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void randomkey() {
        assertThat(blockingKeys.randomkey()).isNull();
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.randomkey()).isEqualTo(key);
    }

    @Test
    void rename() {
        blockingValues.set(key, Person.person7);

        blockingKeys.rename(key, key + "X");
        assertThat(blockingValues.get(key)).isNull();
        assertThat(blockingValues.get(key + "X")).isEqualTo(Person.person7);
        blockingValues.set(key, Person.person4);
        blockingKeys.rename(key + "X", key);
        assertThat(blockingValues.get(key)).isEqualTo(Person.person7);
    }

    @Test
    void renameNonexistentKey() {
        assertThatThrownBy(() -> blockingKeys.rename(key, key + "X")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void renamenx() {
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.renamenx(key, key + "X")).isTrue();
        assertThat(blockingValues.get(key + "X")).isEqualTo(Person.person7);
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.renamenx(key + "X", key)).isFalse();
    }

    @Test
    void renamenxNonexistentKey() {
        assertThatThrownBy(() -> blockingKeys.renamenx(key, key + "X")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void touch() {
        assertThat((long) blockingKeys.touch(key)).isEqualTo(0);
        blockingValues.set(key, Person.person7);
        assertThat((long) blockingKeys.touch(key, "key2")).isEqualTo(1);
    }

    @Test
    void ttl() {
        assertThatThrownBy(() -> blockingKeys.ttl(key)).isInstanceOf(RedisKeyNotFoundException.class);
        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.ttl(key)).isEqualTo(-1);
        blockingKeys.expire(key, 10);
        assertThat(blockingKeys.ttl(key)).isEqualTo(10);
    }

    @Test
    void type() {
        assertThat(blockingKeys.type(key)).isEqualTo(RedisValueType.NONE);

        blockingValues.set(key, Person.person7);
        assertThat(blockingKeys.type(key)).isEqualTo(RedisValueType.STRING);

        blockingDs.hash(String.class, String.class, Person.class).hset(key + "H", "p3", Person.person3);
        assertThat(blockingKeys.type(key + "H")).isEqualTo(RedisValueType.HASH);

        ListCommands<String, String> lists = blockingDs.list(String.class);
        lists.lpush(key + "L", "1");
        assertThat(blockingKeys.type(key + "L")).isEqualTo(RedisValueType.LIST);

        blockingDs.set(String.class, Person.class).sadd(key + "S", Person.person4);
        assertThat(blockingKeys.type(key + "S")).isEqualTo(RedisValueType.SET);

        SortedSetCommands<String, String> ss = blockingDs.sortedSet(String.class);
        ss.zadd(key + "Z", 1, "1");
        assertThat(blockingKeys.type(key + "Z")).isEqualTo(RedisValueType.ZSET);
    }

    @Test
    void scan() {
        blockingValues.set(key, Person.person7);
        KeyScanCursor<String> cursor = blockingKeys.scan();
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.next()).containsExactly(key);
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void scanEmpty() {
        KeyScanCursor<String> cursor = blockingKeys.scan();
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.next()).isEmpty();
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void scanIterableEmpty() {
        KeyScanCursor<String> cursor = blockingKeys.scan();
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.toIterable()).isEmpty();
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void scanWithArgs() {
        blockingValues.set(key, Person.person7);
        KeyScanCursor<String> cursor = blockingKeys.scan(new KeyScanArgs().count(10));
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.next()).containsExactly(key);
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void scanWithType() {
        blockingValues.set("key1", Person.person7);
        blockingDs.list(Person.class).lpush("key2", Person.person7);

        KeyScanCursor<String> cursor = blockingKeys.scan(new KeyScanArgs().type(RedisValueType.STRING));
        assertThat(cursor.next()).containsExactly("key1");

        cursor = blockingKeys.scan(new KeyScanArgs().type(RedisValueType.LIST));
        assertThat(cursor.next()).containsExactly("key2");
    }

    @Test
    void scanMultiple() {
        Set<String> expect = new HashSet<>();
        populateMany(expect);

        KeyScanCursor<String> cursor = blockingKeys.scan(new KeyScanArgs().count(12));

        assertThat(cursor.hasNext()).isTrue();

        Set<String> check = new HashSet<>(cursor.next());

        while (cursor.hasNext()) {
            check.addAll(cursor.next());
        }

        assertThat(check).isEqualTo(expect);
        assertThat(check).hasSize(100);
    }

    @Test
    void scanMultipleAsIterable() {
        Set<String> expect = new HashSet<>();
        populateMany(expect);

        KeyScanCursor<String> cursor = blockingKeys.scan(new KeyScanArgs().count(12));
        Iterable<String> iterable = cursor.toIterable();

        Set<String> check = new HashSet<>(cursor.next());
        for (String k : iterable) {
            check.add(k);
        }

        assertThat(check).isEqualTo(expect);
        assertThat(check).hasSize(100);
    }

    @Test
    void scanMatch() {
        Set<String> expect = new HashSet<>();
        populateMany(expect);
        KeyScanCursor<String> cursor = blockingKeys.scan(new KeyScanArgs().count(200).match(key + "*"));
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.next()).hasSize(expect.size());
        assertThat(cursor.hasNext()).isFalse();
    }

    void populateMany(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            blockingValues.set(key + i, new Person("a", "b" + i));
            expect.add(key + i);
        }
    }

}

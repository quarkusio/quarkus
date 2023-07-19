package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class ValueCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    String value = UUID.randomUUID().toString();
    private ValueCommands<String, String> values;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));

        values = ds.value(String.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(values.getDataSource());
    }

    @Test
    void append() {
        assertThat(values.append(key, value)).isEqualTo(value.length());
        assertThat(values.append(key, "X")).isEqualTo(value.length() + 1);
    }

    @Test
    void get() {
        assertThat(values.get(key)).isNull();
        values.set(key, value);
        assertThat(values.get(key)).isEqualTo(value);
    }

    @Test
    void getbit() {
        assertThat(ds.bitmap(String.class).getbit(key, 0)).isEqualTo(0);
        ds.bitmap(String.class).setbit(key, 0, 1);
        assertThat(ds.bitmap(String.class).getbit(key, 0)).isEqualTo(1);
    }

    @Test
    @RequiresRedis6OrHigher
    void getdel() {
        values.set(key, value);
        assertThat(values.getdel(key)).isEqualTo(value);
        assertThat(values.get(key)).isNull();
    }

    @Test
    @RequiresRedis6OrHigher
    void getex() {
        values.set(key, value);
        assertThat(values.getex(key, new GetExArgs().ex(Duration.ofSeconds(100)))).isEqualTo(value);
        assertThat(ds.key(String.class).ttl(key)).isGreaterThan(1);
        assertThat(values.getex(key, new GetExArgs().persist())).isEqualTo(value);
        assertThat(ds.key(String.class).ttl(key)).isEqualTo(-1);
    }

    @Test
    void getrange() {
        assertThat(values.getrange(key, 0, -1)).isEqualTo("");
        values.set(key, "foobar");
        assertThat(values.getrange(key, 2, 4)).isEqualTo("oba");
        assertThat(values.getrange(key, 3, -1)).isEqualTo("bar");
    }

    @SuppressWarnings("deprecation")
    @Test
    void getset() {
        assertThat(values.getset(key, value)).isNull();
        assertThat(values.getset(key, "two")).isEqualTo(value);
        assertThat(values.get(key)).isEqualTo("two");
    }

    @Test
    void mget() {
        assertThat(values.mget(key)).containsExactly(entry(key, null));
        values.set("one", "1");
        values.set("two", "2");
        assertThat(values.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void mgetWithMissingKey() {
        assertThat(values.mget(key)).containsExactly(entry(key, null));
        values.set("one", "1");
        values.set("two", "2");
        assertThat(values.mget("one", "missing", "two")).containsExactly(entry("one", "1"),
                entry("missing", null), entry("two", "2"));
    }

    @Test
    void mset() {
        assertThat(values.mget("one", "two")).containsExactly(entry("one", null), entry("two", null));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        values.mset(map);
        assertThat(values.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void msetnx() {
        values.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(values.msetnx(map)).isFalse();
        ds.key(String.class).del("one");
        assertThat(values.msetnx(map)).isTrue();
        assertThat(values.get("two")).isEqualTo("2");
    }

    @Test
    void set() {
        KeyCommands<String> keys = ds.key(String.class);
        assertThat(values.get(key)).isNull();
        values.set(key, value);
        assertThat(values.get(key)).isEqualTo(value);

        values.set(key, value, new SetArgs().px(20000));
        values.set(key, value, new SetArgs().ex(10));
        assertThat(values.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        values.set(key, value, new SetArgs().ex(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        values.set(key, value, new SetArgs().px(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        values.set(key, value, new SetArgs().px(10000));
        assertThat(values.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        values.set(key, value, new SetArgs().nx());
        values.set(key, value, new SetArgs().xx());
        assertThat(values.get(key)).isEqualTo(value);

        keys.del(key);
        values.set(key, value, new SetArgs().nx());
        assertThat(values.get(key)).isEqualTo(value);

        keys.del(key);

        values.set(key, value, new SetArgs().px(20000).nx());
        assertThat(values.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key) >= 19).isTrue();
    }

    @Test
    @RequiresRedis6OrHigher
    void setExAt() {
        KeyCommands<String> keys = ds.key(String.class);

        values.set(key, value, new SetArgs().exAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);

        values.set(key, value, new SetArgs().pxAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);
    }

    @Test
    @RequiresRedis6OrHigher
    void setKeepTTL() {
        KeyCommands<String> keys = ds.key(String.class);

        values.set(key, value, new SetArgs().ex(10));
        values.set(key, "value2", new SetArgs().keepttl());
        assertThat(values.get(key)).isEqualTo("value2");
        assertThat(keys.ttl(key) >= 1).isTrue();
    }

    @Test
    void setNegativeEX() {
        assertThatThrownBy(() -> values.set(key, value, new SetArgs().ex(-10))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setNegativePX() {
        assertThatThrownBy(() -> values.set(key, value, new SetArgs().px(-1000))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @RequiresRedis7OrHigher
    void setGet() {
        assertThat(values.setGet(key, value)).isNull();
        assertThat(values.setGet(key, "value2")).isEqualTo(value);
        assertThat(values.get(key)).isEqualTo("value2");
    }

    @Test
    @RequiresRedis7OrHigher
    void setGetWithArgs() {
        KeyCommands<String> keys = ds.key(String.class);

        assertThat(values.setGet(key, value)).isNull();
        assertThat(values.setGet(key, "value2", new SetArgs().ex(100))).isEqualTo(value);
        assertThat(values.get(key)).isEqualTo("value2");
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(10);
    }

    @Test
    void setbit() {
        assertThat(ds.bitmap(String.class).setbit(key, 0, 1)).isEqualTo(0);
        assertThat(ds.bitmap(String.class).setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    void setex() {
        KeyCommands<String> keys = ds.key(String.class);

        values.setex(key, 10, value);
        assertThat(values.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key) >= 9).isTrue();
    }

    @Test
    void psetex() {
        KeyCommands<String> keys = ds.key(String.class);

        values.psetex(key, 20000, value);
        assertThat(values.get(key)).isEqualTo(value);
        assertThat(keys.pttl(key) >= 19000).isTrue();
    }

    @Test
    void setnx() {
        assertThat(values.setnx(key, value)).isTrue();
        assertThat(values.setnx(key, value)).isFalse();
    }

    @Test
    void setrange() {
        assertThat(values.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(values.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(values.get(key)).isEqualTo("foobar");
    }

    @Test
    void strlen() {
        assertThat(values.strlen(key)).isEqualTo(0);
        values.set(key, value);
        assertThat(values.strlen(key)).isEqualTo(value.length());
    }

    @Test
    @RequiresRedis7OrHigher
    void lcs() {
        values.mset(Map.of("key1", "ohmytext", "key2", "mynewtext"));
        assertThat(values.lcs("key1", "key2")).isEqualTo("mytext");

        // LEN parameter
        assertThat(values.lcsLength("key1", "key2")).isEqualTo(6);
    }

    @Test
    void binary() {
        byte[] content = new byte[2048];
        new Random().nextBytes(content);
        ValueCommands<String, byte[]> commands = ds.value(byte[].class);
        commands.set(key, content);
        byte[] bytes = commands.get(key);
        assertThat(bytes).isEqualTo(content);

        // Verify that we do not get through the JSON codec (which would base64 encode the byte[])
        ValueCommands<String, String> cmd = ds.value(String.class);
        String str = cmd.get(key);
        assertThatThrownBy(() -> Json.decodeValue(str, byte[].class)).isInstanceOf(DecodeException.class);
    }

    @Test
    void setWithTypeReference() {
        KeyCommands<String> keys = ds.key(String.class);
        var values = ds.value(new TypeReference<List<Person>>() {
            // Empty on purpose
        });
        assertThat(values.get(key)).isNull();
        List<Person> people = List.of(Person.person1, Person.person2);
        values.set(key, people);
        assertThat(values.get(key)).isEqualTo(people);

        values.set(key, people, new SetArgs().px(20000));
        values.set(key, people, new SetArgs().ex(10));
        assertThat(values.get(key)).isEqualTo(people);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        values.set(key, people, new SetArgs().ex(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        values.set(key, people, new SetArgs().px(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        values.set(key, people, new SetArgs().px(10000));
        assertThat(values.get(key)).isEqualTo(people);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        values.set(key, people, new SetArgs().nx());
        values.set(key, people, new SetArgs().xx());
        assertThat(values.get(key)).isEqualTo(people);

        keys.del(key);
        values.set(key, people, new SetArgs().nx());
        assertThat(values.get(key)).isEqualTo(people);

        keys.del(key);

        values.set(key, people, new SetArgs().px(20000).nx());
        assertThat(values.get(key)).isEqualTo(people);
        assertThat(keys.ttl(key) >= 19).isTrue();
    }
}

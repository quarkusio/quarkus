package io.quarkus.redis.lettuce.runtime.internal.value;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import tools.jackson.core.type.TypeReference;

class LettuceValueCommandsTest extends CommandsTestBase {

    final String value = UUID.randomUUID().toString();

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveValueCommands<String, String> reactiveValues;
    ValueCommands<String, String> blockingValues;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveValues = reactiveDs.value(String.class);
        blockingValues = blockingDs.value(String.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveValues.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingValues.getDataSource());
    }

    @Test
    void append() {
        assertThat(blockingValues.append(key, value)).isEqualTo(value.length());
        assertThat(blockingValues.append(key, "X")).isEqualTo(value.length() + 1);
    }

    @Test
    void get() {
        assertThat(blockingValues.get(key)).isNull();
        blockingValues.set(key, value);
        assertThat(blockingValues.get(key)).isEqualTo(value);
    }

    @Test
    void getbit() {
        assertThat(blockingDs.bitmap(String.class).getbit(key, 0)).isEqualTo(0);
        blockingDs.bitmap(String.class).setbit(key, 0, 1);
        assertThat(blockingDs.bitmap(String.class).getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void getdel() {
        blockingValues.set(key, value);
        assertThat(blockingValues.getdel(key)).isEqualTo(value);
        assertThat(blockingValues.get(key)).isNull();
    }

    @Test
    void getex() {
        blockingValues.set(key, value);
        assertThat(blockingValues.getex(key, new GetExArgs().ex(Duration.ofSeconds(100)))).isEqualTo(value);
        assertThat(blockingDs.key(String.class).ttl(key)).isGreaterThan(1);
        assertThat(blockingValues.getex(key, new GetExArgs().persist())).isEqualTo(value);
        assertThat(blockingDs.key(String.class).ttl(key)).isEqualTo(-1);
    }

    @Test
    void getrange() {
        assertThat(blockingValues.getrange(key, 0, -1)).isEqualTo("");
        blockingValues.set(key, "foobar");
        assertThat(blockingValues.getrange(key, 2, 4)).isEqualTo("oba");
        assertThat(blockingValues.getrange(key, 3, -1)).isEqualTo("bar");
    }

    @SuppressWarnings("deprecation")
    @Test
    void getset() {
        assertThat(blockingValues.getset(key, value)).isNull();
        assertThat(blockingValues.getset(key, "two")).isEqualTo(value);
        assertThat(blockingValues.get(key)).isEqualTo("two");
    }

    @Test
    void mget() {
        assertThat(blockingValues.mget(key)).containsExactly(entry(key, null));
        blockingValues.set("one", "1");
        blockingValues.set("two", "2");
        assertThat(blockingValues.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void mgetWithMissingKey() {
        assertThat(blockingValues.mget(key)).containsExactly(entry(key, null));
        blockingValues.set("one", "1");
        blockingValues.set("two", "2");
        assertThat(blockingValues.mget("one", "missing", "two")).containsExactly(entry("one", "1"),
                entry("missing", null), entry("two", "2"));
    }

    @Test
    void mset() {
        assertThat(blockingValues.mget("one", "two")).containsExactly(entry("one", null), entry("two", null));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        blockingValues.mset(map);
        assertThat(blockingValues.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void msetnx() {
        blockingValues.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(blockingValues.msetnx(map)).isFalse();
        blockingDs.key(String.class).del("one");
        assertThat(blockingValues.msetnx(map)).isTrue();
        assertThat(blockingValues.get("two")).isEqualTo("2");
    }

    @Test
    void set() {
        KeyCommands<String> keys = blockingDs.key(String.class);
        assertThat(blockingValues.get(key)).isNull();
        blockingValues.set(key, value);
        assertThat(blockingValues.get(key)).isEqualTo(value);

        blockingValues.set(key, value, new SetArgs().px(20000));
        blockingValues.set(key, value, new SetArgs().ex(10));
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        blockingValues.set(key, value, new SetArgs().ex(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        blockingValues.set(key, value, new SetArgs().px(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        blockingValues.set(key, value, new SetArgs().px(10000));
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        blockingValues.set(key, value, new SetArgs().nx());
        blockingValues.set(key, value, new SetArgs().xx());
        assertThat(blockingValues.get(key)).isEqualTo(value);

        keys.del(key);
        blockingValues.set(key, value, new SetArgs().nx());
        assertThat(blockingValues.get(key)).isEqualTo(value);

        keys.del(key);

        blockingValues.set(key, value, new SetArgs().px(20000).nx());
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(19);
    }

    @Test
    void setExAt() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        blockingValues.set(key, value, new SetArgs().exAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);

        blockingValues.set(key, value, new SetArgs().pxAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);
    }

    @Test
    void setKeepTTL() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        blockingValues.set(key, value, new SetArgs().ex(10));
        blockingValues.set(key, "value2", new SetArgs().keepttl());
        assertThat(blockingValues.get(key)).isEqualTo("value2");
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void setNegativeEX() {
        assertThatThrownBy(() -> blockingValues.set(key, value, new SetArgs().ex(-10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setNegativePX() {
        assertThatThrownBy(() -> blockingValues.set(key, value, new SetArgs().px(-1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setAndChanged() {
        assertThat(blockingValues.setAndChanged(key, value)).isTrue();
        assertThat(blockingValues.setAndChanged(key, "value2")).isTrue();
        assertThat(blockingValues.get(key)).isEqualTo("value2");
    }

    @Test
    void setAndChangedWithArgs() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        assertThat(blockingValues.setAndChanged(key, value)).isTrue();
        assertThat(blockingValues.setAndChanged(key, "value2", new SetArgs().nx())).isFalse();
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.del(key)).isEqualTo(1);

        assertThat(blockingValues.setAndChanged(key, value, new SetArgs().xx())).isFalse();
        assertThat(blockingValues.get(key)).isNull();
    }

    @Test
    void setGet() {
        assertThat(blockingValues.setGet(key, value)).isNull();
        assertThat(blockingValues.setGet(key, "value2")).isEqualTo(value);
        assertThat(blockingValues.get(key)).isEqualTo("value2");
    }

    @Test
    void setGetWithArgs() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        assertThat(blockingValues.setGet(key, value)).isNull();
        assertThat(blockingValues.setGet(key, "value2", new SetArgs().ex(100))).isEqualTo(value);
        assertThat(blockingValues.get(key)).isEqualTo("value2");
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(10);
    }

    @Test
    void setbit() {
        assertThat(blockingDs.bitmap(String.class).setbit(key, 0, 1)).isEqualTo(0);
        assertThat(blockingDs.bitmap(String.class).setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    void setex() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        blockingValues.setex(key, 10, value);
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);
    }

    @Test
    void psetex() {
        KeyCommands<String> keys = blockingDs.key(String.class);

        blockingValues.psetex(key, 20000, value);
        assertThat(blockingValues.get(key)).isEqualTo(value);
        assertThat(keys.pttl(key)).isGreaterThanOrEqualTo(19000);
    }

    @Test
    void setnx() {
        assertThat(blockingValues.setnx(key, value)).isTrue();
        assertThat(blockingValues.setnx(key, value)).isFalse();
    }

    @Test
    void setrange() {
        assertThat(blockingValues.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(blockingValues.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(blockingValues.get(key)).isEqualTo("foobar");
    }

    @Test
    void strlen() {
        assertThat(blockingValues.strlen(key)).isEqualTo(0);
        blockingValues.set(key, value);
        assertThat(blockingValues.strlen(key)).isEqualTo(value.length());
    }

    @Test
    void lcs() {
        blockingValues.mset(Map.of("key1", "ohmytext", "key2", "mynewtext"));
        assertThat(blockingValues.lcs("key1", "key2")).isEqualTo("mytext");

        // LEN parameter
        assertThat(blockingValues.lcsLength("key1", "key2")).isEqualTo(6);
    }

    @Test
    void binary() {
        byte[] content = new byte[2048];
        new Random().nextBytes(content);
        ValueCommands<String, byte[]> commands = blockingDs.value(byte[].class);
        commands.set(key, content);
        byte[] bytes = commands.get(key);
        assertThat(bytes).isEqualTo(content);

        // Verify that we do not get through the JSON codec (which would base64 encode the byte[])
        ValueCommands<String, String> cmd = blockingDs.value(String.class);
        String str = cmd.get(key);
        assertThatThrownBy(() -> Json.decodeValue(str, byte[].class)).isInstanceOf(DecodeException.class);
    }

    @Test
    void setWithTypeReference() {
        KeyCommands<String> keys = blockingDs.key(String.class);
        var values = blockingDs.value(new TypeReference<List<Person>>() {
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
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(19);
    }

    @Test
    void apiReturnsImmutableCollections() {
        blockingValues.set(key, "value");
        assertImmutable(blockingValues.mget(key, "missing"));
    }

}

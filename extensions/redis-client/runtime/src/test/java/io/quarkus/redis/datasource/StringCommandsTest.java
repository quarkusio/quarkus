package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.string.GetExArgs;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@SuppressWarnings("deprecation")
@RequiresRedis6OrHigher // The ValueCommandsTest verify the behavior with Redis 5
public class StringCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    String value = UUID.randomUUID().toString();
    private StringCommands<String, String> strings;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));

        strings = ds.string(String.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(strings.getDataSource());
    }

    @Test
    void append() {
        assertThat(strings.append(key, value)).isEqualTo(value.length());
        assertThat(strings.append(key, "X")).isEqualTo(value.length() + 1);
    }

    @Test
    void get() {
        assertThat(strings.get(key)).isNull();
        strings.set(key, value);
        assertThat(strings.get(key)).isEqualTo(value);
    }

    @Test
    void getbit() {
        assertThat(ds.bitmap(String.class).getbit(key, 0)).isEqualTo(0);
        ds.bitmap(String.class).setbit(key, 0, 1);
        assertThat(ds.bitmap(String.class).getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void getdel() {
        strings.set(key, value);
        assertThat(strings.getdel(key)).isEqualTo(value);
        assertThat(strings.get(key)).isNull();
    }

    @Test
    void getex() {
        strings.set(key, value);
        assertThat(strings.getex(key, new GetExArgs().ex(Duration.ofSeconds(100)))).isEqualTo(value);
        assertThat(ds.key(String.class).ttl(key)).isGreaterThan(1);
        assertThat(strings.getex(key, new GetExArgs().persist())).isEqualTo(value);
        assertThat(ds.key(String.class).ttl(key)).isEqualTo(-1);
    }

    @Test
    void getrange() {
        assertThat(strings.getrange(key, 0, -1)).isEqualTo("");
        strings.set(key, "foobar");
        assertThat(strings.getrange(key, 2, 4)).isEqualTo("oba");
        assertThat(strings.getrange(key, 3, -1)).isEqualTo("bar");
    }

    @Test
    void getset() {
        assertThat(strings.getset(key, value)).isNull();
        assertThat(strings.getset(key, "two")).isEqualTo(value);
        assertThat(strings.get(key)).isEqualTo("two");
    }

    @Test
    void mget() {
        assertThat(strings.mget(key)).containsExactly(entry(key, null));
        strings.set("one", "1");
        strings.set("two", "2");
        assertThat(strings.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void mgetWithMissingKey() {
        assertThat(strings.mget(key)).containsExactly(entry(key, null));
        strings.set("one", "1");
        strings.set("two", "2");
        assertThat(strings.mget("one", "missing", "two")).containsExactly(entry("one", "1"), entry("missing", null),
                entry("two", "2"));
    }

    @Test
    void mset() {
        assertThat(strings.mget(key)).containsExactly(entry(key, null));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        strings.mset(map);
        assertThat(strings.mget("one", "two")).containsExactly(entry("one", "1"), entry("two", "2"));
    }

    @Test
    void msetnx() {
        strings.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(strings.msetnx(map)).isFalse();
        ds.key(String.class).del("one");
        assertThat(strings.msetnx(map)).isTrue();
        assertThat(strings.get("two")).isEqualTo("2");
    }

    @Test
    void set() {
        KeyCommands<String> keys = ds.key(String.class);
        assertThat(strings.get(key)).isNull();
        strings.set(key, value);
        assertThat(strings.get(key)).isEqualTo(value);

        strings.set(key, value, new SetArgs().px(20000));
        strings.set(key, value, new SetArgs().ex(10));
        assertThat(strings.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        strings.set(key, value, new SetArgs().ex(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        strings.set(key, value, new SetArgs().px(Duration.ofSeconds(10)));
        assertThat(keys.ttl(key)).isBetween(5L, 10L);

        strings.set(key, value, new SetArgs().px(10000));
        assertThat(strings.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key)).isGreaterThanOrEqualTo(9);

        strings.set(key, value, new SetArgs().nx());
        strings.set(key, value, new SetArgs().xx());
        assertThat(strings.get(key)).isEqualTo(value);

        keys.del(key);
        strings.set(key, value, new SetArgs().nx());
        assertThat(strings.get(key)).isEqualTo(value);

        keys.del(key);

        strings.set(key, value, new SetArgs().px(20000).nx());
        assertThat(strings.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key) >= 19).isTrue();
    }

    @Test
    void setExAt() {
        KeyCommands<String> keys = ds.key(String.class);

        strings.set(key, value, new SetArgs().exAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);

        strings.set(key, value, new SetArgs().pxAt(Instant.now().plusSeconds(60)));
        assertThat(keys.ttl(key)).isBetween(50L, 61L);
    }

    @Test
    void setKeepTTL() {
        KeyCommands<String> keys = ds.key(String.class);

        strings.set(key, value, new SetArgs().ex(10));
        strings.set(key, "value2", new SetArgs().keepttl());
        assertThat(strings.get(key)).isEqualTo("value2");
        assertThat(keys.ttl(key) >= 1).isTrue();
    }

    @Test
    void setNegativeEX() {
        assertThatThrownBy(() -> strings.set(key, value, new SetArgs().ex(-10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setNegativePX() {
        assertThatThrownBy(() -> strings.set(key, value, new SetArgs().px(-1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setGet() {
        assertThat(strings.setGet(key, value)).isNull();
        assertThat(strings.setGet(key, "value2")).isEqualTo(value);
        assertThat(strings.get(key)).isEqualTo("value2");
    }

    @Test
    void setGetWithArgs() {
        KeyCommands<String> keys = ds.key(String.class);

        assertThat(strings.setGet(key, value)).isNull();
        assertThat(strings.setGet(key, "value2", new SetArgs().ex(100))).isEqualTo(value);
        assertThat(strings.get(key)).isEqualTo("value2");
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

        strings.setex(key, 10, value);
        assertThat(strings.get(key)).isEqualTo(value);
        assertThat(keys.ttl(key) >= 9).isTrue();
    }

    @Test
    void psetex() {
        KeyCommands<String> keys = ds.key(String.class);

        strings.psetex(key, 20000, value);
        assertThat(strings.get(key)).isEqualTo(value);
        assertThat(keys.pttl(key) >= 19000).isTrue();
    }

    @Test
    void setnx() {
        assertThat(strings.setnx(key, value)).isTrue();
        assertThat(strings.setnx(key, value)).isFalse();
    }

    @Test
    void setrange() {
        assertThat(strings.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(strings.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(strings.get(key)).isEqualTo("foobar");
    }

    @Test
    void strlen() {
        assertThat(strings.strlen(key)).isEqualTo(0);
        strings.set(key, value);
        assertThat(strings.strlen(key)).isEqualTo(value.length());
    }

    @Test
    @RequiresRedis7OrHigher
    void lcs() {
        strings.mset(Map.of("key1", "ohmytext", "key2", "mynewtext"));
        assertThat(strings.lcs("key1", "key2")).isEqualTo("mytext");

        // LEN parameter
        assertThat(strings.lcsLength("key1", "key2")).isEqualTo(6);
    }

    @Test
    void binary() {
        byte[] content = new byte[2048];
        new Random().nextBytes(content);
        StringCommands<String, byte[]> commands = ds.string(byte[].class);
        commands.set(key, content);
        byte[] bytes = commands.get(key);
        assertThat(bytes).isEqualTo(content);
    }
}

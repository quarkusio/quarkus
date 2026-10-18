package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.set.SScanCursor;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

class ScanCommandsMatchTest extends DatasourceTestBase {

    private RedisDataSource ds;
    private static final String KEY = "key-zz";

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(2));
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void testScanToIterable() {
        var valueCommands = ds.value(String.class);
        var keyCommands = ds.key();

        for (int i = 0; i < 10; i++) {
            valueCommands.set("k" + i, "v" + i);
        }
        valueCommands.set("kz", "vz");

        var scan = keyCommands.scan(new KeyScanArgs().count(1).match("k*z"));
        var found = new HashSet<String>();
        for (var e : scan.toIterable()) {
            found.add(e);
        }

        assertEquals(1, found.size());
    }

    @Test
    void testHScanToIterable() {
        var hashCommands = ds.hash(String.class);

        for (int i = 0; i <= 512; i++) {
            hashCommands.hset(KEY, "k" + i, "v" + i);
        }
        hashCommands.hset(KEY, "kz", "vz");

        var scan = hashCommands.hscan(KEY, new ScanArgs().count(10).match("k*z"));
        var found = new HashSet<Map.Entry<String, String>>();
        for (var e : scan.toIterable()) {
            found.add(e);
        }

        assertEquals(1, found.size());
    }

    @Test
    void testSScanToIterable() {
        SetCommands<String, String> setCommands = ds.set(String.class);

        for (int i = 0; i < 512; i++) {
            setCommands.sadd(KEY, "v" + i);
        }
        setCommands.sadd(KEY, "vz");

        SScanCursor<String> sscan = setCommands.sscan(KEY, new ScanArgs().count(10).match("v*z"));
        var founded = new HashSet<String>();
        for (var e : sscan.toIterable()) {
            founded.add(e);
        }

        assertEquals(1, founded.size());
    }

    @Test
    void testZScanToIterable() {
        var sortedSetCommands = ds.sortedSet(String.class);
        for (int i = 0; i <= 512; i++) {
            sortedSetCommands.zadd(KEY, i, "v" + i);
        }
        sortedSetCommands.zadd(KEY, 1000, "vz");

        var zscan = sortedSetCommands.zscan(KEY, new ScanArgs().count(10).match("v*z"));
        var found = new HashSet<ScoredValue<String>>();
        for (var e : zscan.toIterable()) {
            found.add(e);
        }

        assertEquals(1, found.size());
    }
}

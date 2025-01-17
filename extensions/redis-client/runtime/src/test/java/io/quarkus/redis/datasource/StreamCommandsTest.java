package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("xadd")
public class StreamCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private StreamCommands<String, String, Integer> stream;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        stream = ds.stream(Integer.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(stream.getDataSource());
    }

    @Test
    void xreadTest() {
        stream.xadd("my-stream", Map.of("duration", 1532, "event-id", 5, "user-id", 77788));
        stream.xadd("my-stream", Map.of("duration", 1533, "event-id", 6, "user-id", 77788));
        stream.xadd("my-stream", Map.of("duration", 1534, "event-id", 7, "user-id", 77788));

        List<StreamMessage<String, String, Integer>> messages = stream.xread("my-stream", "0-0");
        assertThat(messages).hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo("my-stream");
                    assertThat(m.id()).isNotEmpty().contains("-");
                    assertThat(m.payload()).contains(entry("user-id", 77788)).containsKey("event-id").containsKey("duration");
                });
    }

    @Test
    void xAdd() {
        assertThat(stream.xadd("mystream", Map.of("sensor-id", 1234, "temperature", 19)))
                .isNotBlank().contains("-");

        long now = System.currentTimeMillis();
        assertThat(stream.xadd("mystream", new XAddArgs().id(now + 1000 + "-0"),
                Map.of("sensor-id", 1234, "temperature", 19))).isEqualTo(now + 1000 + "-0");

        for (int i = 0; i < 10; i++) {
            assertThat(stream.xadd("my-second-stream", new XAddArgs().maxlen(5L),
                    Map.of("sensor-id", 1234, "temperature", 19))).isNotBlank();
        }
        assertThat(stream.xlen("my-second-stream")).isEqualTo(5);
    }

    @Test
    @RequiresRedis7OrHigher
    void xAddWithRedis7() {
        assertThat(stream.xadd("mystream", Map.of("sensor-id", 1234, "temperature", 19)))
                .isNotBlank().contains("-");

        long now = System.currentTimeMillis();
        assertThat(stream.xadd("mystream", new XAddArgs().id(now + 1000 + "-0"),
                Map.of("sensor-id", 1234, "temperature", 19))).isEqualTo(now + 1000 + "-0");

        for (int i = 0; i < 10; i++) {
            assertThat(stream.xadd("my-second-stream", new XAddArgs().maxlen(5L),
                    Map.of("sensor-id", 1234, "temperature", 19))).isNotBlank();
        }
        assertThat(stream.xlen("my-second-stream")).isEqualTo(5);

        for (int i = 0; i < 10; i++) {
            assertThat(stream.xadd("my-third-stream", new XAddArgs().minid("12345-0").nearlyExactTrimming()
                    .limit(3).id("12346-" + i),
                    Map.of("sensor-id", 1234, "temperature", 19))).isNotBlank();
        }
        assertThat(stream.xlen("my-third-stream")).isEqualTo(10);
        assertThat(stream.xadd("another", new XAddArgs().nomkstream(), Map.of("foo", 12))).isNull();
    }

    @Test
    void xLen() {
        assertThat(stream.xlen("missing")).isEqualTo(0);
        assertThat(stream.xadd("mystream", Map.of("sensor-id", 1234, "temperature", 19)))
                .isNotBlank().contains("-");
        assertThat(stream.xlen("mystream")).isEqualTo(1);
    }

    @Test
    void xRangeAndxRevRange() {
        List<String> ids = new ArrayList<>();
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 3; i++) {
            ids.add(stream.xadd(key, payload));
        }
        assertThat(stream.xlen(key)).isEqualTo(3);

        assertThat(stream.xrange(key, StreamRange.of("-", "+")))
                .hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xrange(key, StreamRange.of(ids.get(1), ids.get(2))))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xrange(key, StreamRange.of("-", "+"), 2))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xrevrange(key, StreamRange.of("+", "-")))
                .hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xrevrange(key, StreamRange.of(ids.get(2), ids.get(1))))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xrevrange(key, StreamRange.of("+", "-"), 2))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.id()).isNotBlank();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

    }

    @Test
    void xReadWithAndWithoutCount() {
        List<String> ids1 = new ArrayList<>();
        List<String> ids2 = new ArrayList<>();
        String key2 = key + "2";
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 3; i++) {
            ids1.add(stream.xadd(key, payload));
            ids2.add(stream.xadd(key2, payload));
        }
        assertThat(stream.xlen(key)).isEqualTo(3);
        assertThat(stream.xlen(key2)).isEqualTo(3);

        assertThat(stream.xread(key, "0", new XReadArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(ids1).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xread(key2, "0"))
                .hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key2);
                    assertThat(ids2).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });
    }

    @Test
    void xReadMultipleStreams() {
        List<String> ids = new ArrayList<>();
        String key2 = key + "2";
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 3; i++) {
            ids.add(stream.xadd(key, payload));
            ids.add(stream.xadd(key2, payload));
        }
        assertThat(stream.xlen(key)).isEqualTo(3);
        assertThat(stream.xlen(key2)).isEqualTo(3);

        assertThat(stream.xread(Map.of(key2, "0", key, "0")))
                .hasSize(6)
                .allSatisfy(m -> {
                    assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xread(Map.of(key2, "0", key, "0"), new XReadArgs().count(2)))
                .hasSize(4) // the count is per stream
                .allSatisfy(m -> {
                    assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });
    }

    @Test
    void xReadBlocking() throws InterruptedException {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            assertThat(stream.xread(key, "$", new XReadArgs().block(Duration.ofSeconds(10))))
                    .isNotEmpty()
                    .allSatisfy(m -> {
                        assertThat(m.key()).isEqualTo(key);
                        assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    });
            latch.countDown();
        }).start();

        await()
                .pollDelay(10, TimeUnit.MILLISECONDS)
                .until(() -> {
                    stream.xadd(key, payload);
                    return latch.getCount() == 0;
                });

    }

    @Test
    void xReadBlockingMultipleStreams() {
        String key2 = key + "2";
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            assertThat(stream.xread(Map.of(key, "$", key2, "$"), new XReadArgs().block(Duration.ofSeconds(10))))
                    .isNotEmpty()
                    .allSatisfy(m -> {
                        assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
                        assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    });
            latch.countDown();
        }).start();

        await()
                .pollDelay(10, TimeUnit.MILLISECONDS)
                .until(() -> {
                    stream.xadd(key2, payload);
                    stream.xadd(key, payload);
                    return latch.getCount() == 0;
                });
    }

    @Test
    void consumerGroupTests() {
        String g1 = "my-group";
        stream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        String g2 = "my-group-2";
        stream.xgroupCreate(key, g2, "$");
        String g3 = "my-group-3";
        String key2 = key + "2";
        stream.xgroupCreate(key, g3, "$");
        stream.xgroupCreate(key2, g3, "$", new XGroupCreateArgs().mkstream());

        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 5; i++) {
            stream.xadd(key, payload);
            stream.xadd(key2, payload);
        }

        assertThat(stream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(1)))
                .hasSize(1)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                });

        assertThat(stream.xreadgroup(g1, "c2", key, ">"))
                .hasSize(4)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    assertThat(stream.xack(m.key(), g1, m.id())).isEqualTo(1);
                });

        assertThat(stream.xreadgroup(g2, "c2", key, ">"))
                .hasSize(5)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    assertThat(stream.xack(m.key(), g2, m.id())).isEqualTo(1);
                });

        assertThat(stream.xreadgroup(g2, "c2", key, ">"))
                .hasSize(0);

        assertThat(stream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"), new XReadGroupArgs().count(1)))
                .hasSize(2); // 1 per stream
        assertThat(stream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"),
                new XReadGroupArgs().block(Duration.ofSeconds(1)).noack()))
                .hasSize(8);
        assertThat(stream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">")))
                .hasSize(0);
    }

    @Test
    void consumerGroupTestsBlocking() {
        String g1 = "my-group";
        stream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        String g3 = "my-group-3";
        String key2 = key + "2";
        stream.xgroupCreate(key, g3, "$");
        stream.xgroupCreate(key2, g3, "$", new XGroupCreateArgs().mkstream());

        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            assertThat(stream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().block(Duration.ofSeconds(10)).count(1)))
                    .isNotEmpty()
                    .allSatisfy(m -> {
                        assertThat(m.key()).isEqualTo(key);
                        assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    });
            latch.countDown();
        }).start();

        stream.xadd(key, payload);

        await()
                .pollDelay(10, TimeUnit.MILLISECONDS)
                .until(() -> {
                    stream.xadd(key, payload);
                    return latch.getCount() == 0;
                });
        CountDownLatch latch2 = new CountDownLatch(1);

        new Thread(() -> {
            assertThat(stream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"),
                    new XReadGroupArgs().block(Duration.ofSeconds(10))))
                    .isNotEmpty();
            latch2.countDown();
        }).start();

        stream.xadd(key2, payload);

        await()
                .pollDelay(10, TimeUnit.MILLISECONDS)
                .until(() -> {
                    stream.xadd(key2, payload);
                    return latch.getCount() == 0;
                });
    }

    @Test
    void xClaim() throws InterruptedException {
        String g1 = "my-group";
        stream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());

        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 5; i++) {
            stream.xadd(key, payload);
        }

        List<String> pending = new ArrayList<>();
        assertThat(stream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    // Do not ack
                    pending.add(m.id());
                });

        List<String> read = new ArrayList<>();
        assertThat(stream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    read.add(m.id());
                });

        assertThat(stream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        // Make sure that the message are pending for a bit of time before claiming the ownership
        Thread.sleep(5);

        assertThat(stream.xclaim(key, g1, "c2", Duration.ofMillis(1), pending.toArray(new String[0])))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    stream.xack(key, g1, m.id());
                });

        assertThat(stream.xreadgroup(g1, "c1", key, ">")).hasSize(1);
        assertThat(stream.xreadgroup(g1, "c2", key, ">")).hasSize(0);
    }

    @Test
    void xClaimWithArgs() throws InterruptedException {
        String g1 = "my-group";
        stream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());

        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 5; i++) {
            stream.xadd(key, payload);
        }

        List<String> pending = new ArrayList<>();
        assertThat(stream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    // Do not ack
                    pending.add(m.id());
                });

        List<String> read = new ArrayList<>();
        assertThat(stream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    read.add(m.id());
                });

        assertThat(stream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        // Make sure that the message are pending for a bit of time before claiming the ownership
        Thread.sleep(5);

        assertThat(stream.xclaim(key, g1, "c2", Duration.ofMillis(1), new XClaimArgs()
                .force().retryCount(5).idle(Duration.ofMillis(1)).justId(), pending.toArray(new String[0])))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).isEmpty(); // Justid
                    stream.xack(key, g1, m.id());
                });

        assertThat(stream.xreadgroup(g1, "c1", key, ">")).hasSize(1);
        assertThat(stream.xreadgroup(g1, "c2", key, ">")).hasSize(0);
    }

    @Test
    @RequiresRedis6OrHigher
    void xAutoClaim() throws InterruptedException {
        String g1 = "my-group";
        stream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());

        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 10; i++) {
            stream.xadd(key, payload);
        }

        assertThat(stream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(4)))
                .hasSize(4)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    // Do not ack
                });

        List<String> read = new ArrayList<>();
        assertThat(stream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2)))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    read.add(m.id());
                });

        assertThat(stream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        // Make sure that the message are pending for a bit of time before claiming the ownership
        Thread.sleep(5);

        var claimed = stream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), "0", 1);
        assertThat(claimed.getMessages())
                .hasSize(1)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    stream.xack(key, g1, m.id());
                });

        claimed = stream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), "0", 2, true);
        assertThat(claimed.getMessages())
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).isEmpty();
                    stream.xack(key, g1, m.id());
                });

        claimed = stream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), claimed.getId());
        assertThat(claimed.getMessages())
                .hasSize(1)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(payload);
                    stream.xack(key, g1, m.id());
                });
    }

    @Test
    @RequiresRedis6OrHigher
    void xTrim() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        var l = stream.xtrim(key, new XTrimArgs().maxlen(50));

        assertThat(l).isEqualTo(50);
        assertThat(stream.xlen(key)).isEqualTo(50);

        var list = stream.xrange(key, StreamRange.of("-", "+"));
        var id = list.get(10).id();
        l = stream.xtrim(key, id);

        assertThat(l).isEqualTo(10);
        assertThat(stream.xlen(key)).isEqualTo(40);
    }

    @Test
    void xDel() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        var list = stream.xrange(key, StreamRange.of("-", "+"));

        assertThat(stream.xdel(key, list.get(0).id(), list.get(3).id(), "12345-01")).isEqualTo(2);
        assertThat(stream.xlen(key)).isEqualTo(98);
    }

    @Test
    @RequiresRedis6OrHigher
    void xGroupCreateAndDeleteConsumer() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "g1", "0");
        assertThat(stream.xgroupCreateConsumer(key, "g1", "c1")).isTrue();
        assertThat(stream.xgroupCreateConsumer(key, "g1", "c2")).isTrue();
        assertThat(stream.xgroupCreateConsumer(key, "g1", "c1")).isFalse();

        assertThatThrownBy(() -> stream.xgroupCreateConsumer(key, "missing", "c3"))
                .hasMessageContaining("missing");

        assertThat(stream.xgroupDelConsumer(key, "g1", "c1")).isEqualTo(0);

        assertThat(stream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);

        assertThat(stream.xgroupDelConsumer(key, "g1", "c2")).isEqualTo(10);

        assertThat(stream.xgroupDestroy(key, "g1")).isTrue();

    }

    @Test
    void xGroupSetId() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(stream.xadd(key, payload));
        }

        stream.xgroupCreate(key, "g1", "0");

        assertThat(stream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);

        stream.xgroupSetId(key, "g1", ids.get(50));

        assertThat(stream.xreadgroup("g1", "c2", key, ">")).hasSize(49);

    }

    @Test
    @RequiresRedis7OrHigher
    void xGroupSetIdWithArgs() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(stream.xadd(key, payload));
        }

        stream.xgroupCreate(key, "g1", "0");

        assertThat(stream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);

        stream.xgroupSetId(key, "g1", ids.get(50), new XGroupSetIdArgs().entriesRead(1234));

        assertThat(stream.xreadgroup("g1", "c2", key, ">")).hasSize(49);
    }

    @Test
    void xPendingSummaryTest() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);

        stream.xadd(key, payload);
        stream.xtrim(key, new XTrimArgs().maxlen(0));

        stream.xgroupCreate(key, "my-group", "0-0");

        XPendingSummary summaryEmpty = stream.xpending(key, "my-group");
        assertThat(summaryEmpty.getPendingCount()).isEqualTo(0);
        assertThat(summaryEmpty.getHighestId()).isNull();
        assertThat(summaryEmpty.getLowestId()).isNull();
        assertThat(summaryEmpty.getConsumers()).isEmpty();

        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        List<StreamMessage<String, String, Integer>> messages = stream.xreadgroup("my-group", "consumer-123", key, ">");
        assertThat(messages).hasSize(100);

        XPendingSummary summary = stream.xpending(key, "my-group");
        assertThat(summary.getPendingCount()).isEqualTo(100L);
        assertThat(summary.getHighestId()).isNotNull();
        assertThat(summary.getLowestId()).isNotNull();
        assertThat(summary.getConsumers()).containsExactly(entry("consumer-123", 100L));
    }

    @Test
    void xPendingSummaryTestWithTwoConsumers() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        List<StreamMessage<String, String, Integer>> m1 = stream.xreadgroup("my-group", "consumer-1", key, ">");
        List<StreamMessage<String, String, Integer>> m2 = stream.xreadgroup("my-group", "consumer-2", key, ">");
        assertThat(m1.size() + m2.size()).isEqualTo(100);

        XPendingSummary summary = stream.xpending(key, "my-group");
        assertThat(summary.getPendingCount()).isEqualTo(100L);
        assertThat(summary.getHighestId()).isNotNull();
        assertThat(summary.getLowestId()).isNotNull();
        assertThat(summary.getConsumers()).containsOnlyKeys("consumer-1"); // The second didn't have the chance to poll
    }

    @Test
    void xPendingExtendedTest() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        List<StreamMessage<String, String, Integer>> messages = stream.xreadgroup("my-group", "consumer-123", key, ">");
        assertThat(messages).hasSize(100);

        List<PendingMessage> pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10);
        assertThat(pending).hasSize(10);
        assertThat(pending).allSatisfy(msg -> {
            assertThat(msg.getMessageId()).isNotNull();
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery()).isNotNull();
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });
    }

    @Test
    void xPendingExtendedWithConsumerTest() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        stream.xreadgroup("my-group", "consumer-123", key, ">");
        stream.xreadgroup("my-group", "consumer-456", key, ">");

        List<PendingMessage> pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().consumer("consumer-123"));
        assertThat(pending).hasSize(10);
        assertThat(pending).allSatisfy(msg -> {
            assertThat(msg.getMessageId()).isNotNull();
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery()).isNotNull();
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });

        pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10, new XPendingArgs().consumer("consumer-456"));
        assertThat(pending).isEmpty();

        pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().consumer("consumer-missing"));
        assertThat(pending).isEmpty();
    }

    @Test
    void xPendingExtendedTestWithConsumerAndIdle() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        stream.xreadgroup("my-group", "consumer-123", key, ">");
        stream.xreadgroup("my-group", "consumer-456", key, ">");
    }

    @Test
    @RequiresRedis6OrHigher
    void xPendingExtendedTestWithConsumerAndIdleWithRedis6() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        stream.xreadgroup("my-group", "consumer-123", key, ">");
        stream.xreadgroup("my-group", "consumer-456", key, ">");

        AtomicReference<List<PendingMessage>> reference = new AtomicReference<>();
        await().untilAsserted(() -> {
            List<PendingMessage> pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10, new XPendingArgs()
                    .idle(Duration.ofSeconds(1))
                    .consumer("consumer-123"));
            assertThat(pending).hasSize(10);
            reference.set(pending);
        });
        assertThat(reference.get()).allSatisfy(msg -> {
            assertThat(msg.getMessageId()).isNotNull();
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery()).isNotNull();
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });
    }

    @Test
    @RequiresRedis6OrHigher
    void xPendingExtendedTestWithIdle() {
        Map<String, Integer> payload = Map.of("sensor-id", 1234, "temperature", 19);
        for (int i = 0; i < 100; i++) {
            stream.xadd(key, payload);
        }

        stream.xgroupCreate(key, "my-group", "0-0");
        stream.xreadgroup("my-group", "consumer-123", key, ">");

        AtomicReference<List<PendingMessage>> reference = new AtomicReference<>();
        await().untilAsserted(() -> {
            List<PendingMessage> pending = stream.xpending(key, "my-group", StreamRange.of("-", "+"), 10, new XPendingArgs()
                    .idle(Duration.ofSeconds(1)));
            assertThat(pending).hasSize(10);
            reference.set(pending);
        });
        assertThat(reference.get()).allSatisfy(msg -> {
            assertThat(msg.getMessageId()).isNotNull();
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery()).isNotNull();
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });
    }

    @Test
    void streamWithTypeReference() {
        var stream = ds.stream(new TypeReference<List<Integer>>() {
            // Empty on purpose
        });
        stream.xadd("my-stream", Map.of("duration", List.of(1532), "event-id", List.of(5), "user-id", List.of(77788)));
        stream.xadd("my-stream", Map.of("duration", List.of(1533), "event-id", List.of(6), "user-id", List.of(77788)));
        stream.xadd("my-stream", Map.of("duration", List.of(1534), "event-id", List.of(7), "user-id", List.of(77788)));

        List<StreamMessage<String, String, List<Integer>>> messages = stream.xread("my-stream", "0-0");
        assertThat(messages).hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo("my-stream");
                    assertThat(m.id()).isNotEmpty().contains("-");
                    assertThat(m.payload()).contains(entry("user-id", List.of(77788))).containsKey("event-id")
                            .containsKey("duration");
                });
    }

}

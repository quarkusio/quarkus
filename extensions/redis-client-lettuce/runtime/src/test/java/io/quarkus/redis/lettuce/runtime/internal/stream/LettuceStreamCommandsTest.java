package io.quarkus.redis.lettuce.runtime.internal.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.stream.ClaimedMessages;
import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
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
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import tools.jackson.core.type.TypeReference;

class LettuceStreamCommandsTest extends CommandsTestBase {

    private static final Map<String, Integer> PAYLOAD = Map.of("sensor-id", 1234, "temperature", 19);

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveStreamCommands<String, String, Integer> reactiveStream;
    StreamCommands<String, String, Integer> blockingStream;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveStream = reactiveDs.stream(String.class, String.class, Integer.class);
        blockingStream = blockingDs.stream(Integer.class);
    }

    @Test
    void getDataSource() {
        // ReactiveStreamCommands does not expose getDataSource upstream; the Lettuce impl adds ReactiveRedisCommands
        assertThat(reactiveStream).isInstanceOfSatisfying(ReactiveRedisCommands.class,
                commands -> assertThat(commands.getDataSource()).isEqualTo(reactiveDs));
        assertThat(blockingDs).isEqualTo(blockingStream.getDataSource());
    }

    @Test
    void xreadTest() {
        blockingStream.xadd("my-stream", Map.of("duration", 1532, "event-id", 5, "user-id", 77788));
        blockingStream.xadd("my-stream", Map.of("duration", 1533, "event-id", 6, "user-id", 77788));
        blockingStream.xadd("my-stream", Map.of("duration", 1534, "event-id", 7, "user-id", 77788));

        List<StreamMessage<String, String, Integer>> messages = blockingStream.xread("my-stream", "0-0");
        assertThat(messages).hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo("my-stream");
                    assertThat(m.id()).isNotEmpty().contains("-");
                    assertThat(m.payload()).contains(entry("user-id", 77788)).containsKey("event-id").containsKey("duration");
                    assertThat(m.durationSinceLastDelivery()).isNull();
                    assertThat(m.deliveryCount()).isZero();
                });
    }

    @Test
    void xAdd() {
        assertThat(blockingStream.xadd("mystream", PAYLOAD)).isNotBlank().contains("-");

        long now = System.currentTimeMillis();
        assertThat(blockingStream.xadd("mystream", new XAddArgs().id(now + 1000 + "-0"), PAYLOAD)).isEqualTo(now + 1000 + "-0");

        for (int i = 0; i < 10; i++) {
            assertThat(blockingStream.xadd("my-second-stream", new XAddArgs().maxlen(5L), PAYLOAD)).isNotBlank();
        }
        assertThat(blockingStream.xlen("my-second-stream")).isEqualTo(5);

        for (int i = 0; i < 10; i++) {
            assertThat(blockingStream.xadd("my-third-stream", new XAddArgs().minid("12345-0").nearlyExactTrimming()
                    .limit(3).id("12346-" + i), PAYLOAD)).isNotBlank();
        }
        assertThat(blockingStream.xlen("my-third-stream")).isEqualTo(10);

        assertThat(blockingStream.xadd("another", new XAddArgs().nomkstream(), Map.of("foo", 12))).isNull();
    }

    @Test
    void xAddWithNullValue() {
        Map<String, Integer> payload = new java.util.HashMap<>();
        payload.put("present", 1);
        payload.put("absent", null);
        blockingStream.xadd(key, payload);
        // the Vert.x backend sends a null value as the string "null", which does not decode to an Integer
        var strings = blockingDs.stream(String.class);
        assertThat(strings.xread(key, "0").get(0).payload()).containsEntry("absent", "null").containsEntry("present", "1");
    }

    @Test
    void xLen() {
        assertThat(blockingStream.xlen("missing")).isEqualTo(0);
        assertThat(blockingStream.xadd("mystream", PAYLOAD)).isNotBlank().contains("-");
        assertThat(blockingStream.xlen("mystream")).isEqualTo(1);
    }

    @Test
    void xRangeAndxRevRange() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(blockingStream.xadd(key, PAYLOAD));
        }
        assertThat(blockingStream.xlen(key)).isEqualTo(3);

        assertThat(blockingStream.xrange(key, StreamRange.of("-", "+"))).hasSize(3).allSatisfy(m -> assertMessage(m, ids));
        assertThat(blockingStream.xrange(key, StreamRange.of(ids.get(1), ids.get(2)))).hasSize(2)
                .allSatisfy(m -> assertMessage(m, ids));
        assertThat(blockingStream.xrange(key, StreamRange.of("-", "+"), 2)).hasSize(2).allSatisfy(m -> assertMessage(m, ids));
        // exclusive bound
        assertThat(blockingStream.xrange(key, StreamRange.of("(" + ids.get(0), "+"))).extracting(StreamMessage::id)
                .containsExactly(ids.get(1), ids.get(2));

        assertThat(blockingStream.xrevrange(key, StreamRange.of("+", "-"))).hasSize(3).allSatisfy(m -> assertMessage(m, ids));
        assertThat(blockingStream.xrevrange(key, StreamRange.of("+", "-"))).extracting(StreamMessage::id)
                .containsExactly(ids.get(2), ids.get(1), ids.get(0));
        assertThat(blockingStream.xrevrange(key, StreamRange.of(ids.get(2), ids.get(1)))).hasSize(2)
                .allSatisfy(m -> assertMessage(m, ids));
        assertThat(blockingStream.xrevrange(key, StreamRange.of("+", "-"), 2)).hasSize(2)
                .allSatisfy(m -> assertMessage(m, ids));
    }

    @Test
    void xReadWithAndWithoutCount() {
        List<String> ids1 = new ArrayList<>();
        List<String> ids2 = new ArrayList<>();
        String key2 = key + "2";
        for (int i = 0; i < 3; i++) {
            ids1.add(blockingStream.xadd(key, PAYLOAD));
            ids2.add(blockingStream.xadd(key2, PAYLOAD));
        }
        assertThat(blockingStream.xread(key, "0", new XReadArgs().count(2))).hasSize(2).allSatisfy(m -> assertMessage(m, ids1));
        assertThat(blockingStream.xread(key2, "0")).hasSize(3).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key2);
            assertThat(ids2).contains(m.id());
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });
        assertThat(blockingStream.xread("missing", "0")).isEmpty();
    }

    @Test
    void xReadMultipleStreams() {
        List<String> ids = new ArrayList<>();
        String key2 = key + "2";
        for (int i = 0; i < 3; i++) {
            ids.add(blockingStream.xadd(key, PAYLOAD));
            ids.add(blockingStream.xadd(key2, PAYLOAD));
        }
        assertThat(blockingStream.xread(Map.of(key2, "0", key, "0"))).hasSize(6).allSatisfy(m -> {
            assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
            assertThat(ids).contains(m.id());
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });
        assertThat(blockingStream.xread(Map.of(key2, "0", key, "0"), new XReadArgs().count(2)))
                .hasSize(4) // the count is per stream
                .allSatisfy(m -> {
                    assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
                    assertThat(ids).contains(m.id());
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
                });
    }

    @Test
    void xReadBlocking() throws InterruptedException {
        AtomicReference<List<StreamMessage<String, String, Integer>>> read = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            read.set(blockingStream.xread(key, "$", new XReadArgs().block(Duration.ofSeconds(10))));
            latch.countDown();
        }).start();
        feedUntil(latch, () -> blockingStream.xadd(key, PAYLOAD));
        assertThat(read.get()).isNotEmpty().allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });
    }

    @Test
    void xReadBlockingMultipleStreams() throws InterruptedException {
        String key2 = key + "2";
        AtomicReference<List<StreamMessage<String, String, Integer>>> read = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            read.set(blockingStream.xread(Map.of(key, "$", key2, "$"), new XReadArgs().block(Duration.ofSeconds(10))));
            latch.countDown();
        }).start();
        feedUntil(latch, () -> {
            blockingStream.xadd(key2, PAYLOAD);
            blockingStream.xadd(key, PAYLOAD);
        });
        assertThat(read.get()).isNotEmpty().allSatisfy(m -> {
            assertThat(m.key().equals(key) || m.key().equals(key2)).isTrue();
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });
    }

    @Test
    void xReadBlockingTimesOutWithAnEmptyResult() {
        // longer than the data source timeout would be if it were applied to the blocking command
        assertThat(blockingStream.xread(key, "$", new XReadArgs().block(Duration.ofMillis(200)))).isEmpty();
        assertThat(reactiveStream.xread(Map.of(key, "$"), new XReadArgs().block(Duration.ofMillis(200)))
                .await().atMost(TIMEOUT)).isEmpty();
    }

    @Test
    void consumerGroupTests() {
        String g1 = "my-group";
        blockingStream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        String g2 = "my-group-2";
        blockingStream.xgroupCreate(key, g2, "$");
        String g3 = "my-group-3";
        String key2 = key + "2";
        blockingStream.xgroupCreate(key, g3, "$");
        blockingStream.xgroupCreate(key2, g3, "$", new XGroupCreateArgs().mkstream());

        for (int i = 0; i < 5; i++) {
            blockingStream.xadd(key, PAYLOAD);
            blockingStream.xadd(key2, PAYLOAD);
        }

        assertThat(blockingStream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(1))).hasSize(1).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">")).hasSize(4).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
            assertThat(blockingStream.xack(m.key(), g1, m.id())).isEqualTo(1);
        });
        assertThat(blockingStream.xreadgroup(g2, "c2", key, ">")).hasSize(5).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
            assertThat(blockingStream.xack(m.key(), g2, m.id())).isEqualTo(1);
        });
        assertThat(blockingStream.xreadgroup(g2, "c2", key, ">")).hasSize(0);

        assertThat(blockingStream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"), new XReadGroupArgs().count(1)))
                .hasSize(2); // 1 per stream
        assertThat(blockingStream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"),
                new XReadGroupArgs().block(Duration.ofSeconds(1)).noack())).hasSize(8);
        assertThat(blockingStream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"))).hasSize(0);
    }

    @Test
    void consumerGroupTestsBlocking() throws InterruptedException {
        String g1 = "my-group";
        blockingStream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        String g3 = "my-group-3";
        String key2 = key + "2";
        blockingStream.xgroupCreate(key, g3, "$");
        blockingStream.xgroupCreate(key2, g3, "$", new XGroupCreateArgs().mkstream());

        AtomicReference<List<StreamMessage<String, String, Integer>>> read = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            read.set(
                    blockingStream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().block(Duration.ofSeconds(10)).count(1)));
            latch.countDown();
        }).start();
        feedUntil(latch, () -> blockingStream.xadd(key, PAYLOAD));
        assertThat(read.get()).isNotEmpty().allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
        });

        AtomicReference<List<StreamMessage<String, String, Integer>>> read2 = new AtomicReference<>();
        CountDownLatch latch2 = new CountDownLatch(1);
        new Thread(() -> {
            read2.set(blockingStream.xreadgroup(g3, "c1", Map.of(key, ">", key2, ">"),
                    new XReadGroupArgs().block(Duration.ofSeconds(10))));
            latch2.countDown();
        }).start();
        feedUntil(latch2, () -> blockingStream.xadd(key2, PAYLOAD));
        assertThat(read2.get()).isNotEmpty();
    }

    @Test
    void xClaim() throws InterruptedException {
        String g1 = "my-group";
        List<String> pending = readWithoutAck(g1, "c1", 2);

        List<String> read = new ArrayList<>();
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2))).hasSize(2)
                .allSatisfy(m -> read.add(m.id()));
        assertThat(blockingStream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        // Make sure that the message are pending for a bit of time before claiming the ownership
        Thread.sleep(5);
        assertThat(blockingStream.xclaim(key, g1, "c2", Duration.ofMillis(1), pending.toArray(new String[0]))).hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
                    blockingStream.xack(key, g1, m.id());
                });
        assertThat(blockingStream.xreadgroup(g1, "c1", key, ">")).hasSize(1);
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">")).hasSize(0);
    }

    @Test
    void xClaimWithArgs() throws InterruptedException {
        String g1 = "my-group";
        List<String> pending = readWithoutAck(g1, "c1", 2);

        List<String> read = new ArrayList<>();
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2))).hasSize(2)
                .allSatisfy(m -> read.add(m.id()));
        assertThat(blockingStream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        Thread.sleep(5);
        XClaimArgs args = new XClaimArgs().force().retryCount(5).idle(Duration.ofMillis(1)).justId();
        assertThat(blockingStream.xclaim(key, g1, "c2", Duration.ofMillis(1), args, pending.toArray(new String[0]))).hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).isEmpty(); // JUSTID
                    blockingStream.xack(key, g1, m.id());
                });
        assertThat(blockingStream.xreadgroup(g1, "c1", key, ">")).hasSize(1);
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">")).hasSize(0);
    }

    @Test
    void xClaimReactive() throws InterruptedException {
        String g1 = "my-group";
        List<String> pending = readWithoutAck(g1, "c1", 2);
        Thread.sleep(5);

        List<StreamMessage<String, String, Integer>> claimed = reactiveStream
                .xclaim(key, g1, "c2", Duration.ofMillis(1), pending.toArray(new String[0])).await().atMost(TIMEOUT);
        assertThat(claimed).extracting(StreamMessage::id).containsExactlyElementsOf(pending);
        assertThat(blockingStream.xpending(key, g1).getConsumers()).containsEntry("c2", 2L).doesNotContainKey("c1");

        assertThat(reactiveStream.xclaim(key, g1, "c3", Duration.ofHours(1), pending.toArray(new String[0]))
                .await().atMost(TIMEOUT)).isEmpty();
        assertThat(reactiveStream.xclaim(key, g1, "c3", Duration.ZERO, "0-1").await().atMost(TIMEOUT)).isEmpty();
    }

    @Test
    void xReadGroupWithClaim() throws InterruptedException {
        assumeTrue(redisVersionAtLeast(8, 4), "XREADGROUP CLAIM requires Redis 8.4");
        String g1 = "my-group";
        blockingStream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        for (int i = 0; i < 5; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        // c1 reads 3 messages but does not ack them
        assertThat(blockingStream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(3))).hasSize(3);
        Thread.sleep(50);

        // c2 reads remaining messages and claims c1's idle messages in one call
        List<StreamMessage<String, String, Integer>> messages = blockingStream.xreadgroup(g1, "c2", key, ">",
                new XReadGroupArgs().claim(Duration.ofMillis(10)));
        assertThat(messages).hasSize(5);
        assertThat(messages.stream().filter(m -> m.deliveryCount() > 0)).hasSize(3).allSatisfy(m -> {
            assertThat(m.durationSinceLastDelivery()).isNotNull();
            assertThat(m.durationSinceLastDelivery().toMillis()).isGreaterThan(0);
        });
        // CLAIM reports the idle time for the never-delivered entries as well
        assertThat(messages.stream().filter(m -> m.deliveryCount() == 0)).hasSize(2)
                .allSatisfy(m -> assertThat(m.durationSinceLastDelivery()).isNotNull());
    }

    @Test
    void xAutoClaim() throws InterruptedException {
        String g1 = "my-group";
        blockingStream.xgroupCreate(key, g1, "$", new XGroupCreateArgs().mkstream());
        for (int i = 0; i < 10; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        assertThat(blockingStream.xreadgroup(g1, "c1", key, ">", new XReadGroupArgs().count(4))).hasSize(4);
        List<String> read = new ArrayList<>();
        assertThat(blockingStream.xreadgroup(g1, "c2", key, ">", new XReadGroupArgs().count(2))).hasSize(2)
                .allSatisfy(m -> read.add(m.id()));
        assertThat(blockingStream.xack(key, g1, read.toArray(new String[0]))).isEqualTo(2);

        Thread.sleep(5);
        var claimed = blockingStream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), "0", 1);
        assertThat(claimed.getMessages()).hasSize(1).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
            blockingStream.xack(key, g1, m.id());
        });
        claimed = blockingStream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), "0", 2, true);
        assertThat(claimed.getMessages()).hasSize(2).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).isEmpty();
            blockingStream.xack(key, g1, m.id());
        });
        claimed = blockingStream.xautoclaim(key, g1, "c2", Duration.ofMillis(1), claimed.getId());
        assertThat(claimed.getMessages()).hasSize(1).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo(key);
            assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
            blockingStream.xack(key, g1, m.id());
        });
    }

    @Test
    void xTrim() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        assertThat(blockingStream.xtrim(key, new XTrimArgs().maxlen(50))).isEqualTo(50);
        assertThat(blockingStream.xlen(key)).isEqualTo(50);

        var list = blockingStream.xrange(key, StreamRange.of("-", "+"));
        assertThat(blockingStream.xtrim(key, list.get(10).id())).isEqualTo(10);
        assertThat(blockingStream.xlen(key)).isEqualTo(40);
    }

    @Test
    void xDel() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        var list = blockingStream.xrange(key, StreamRange.of("-", "+"));
        assertThat(blockingStream.xdel(key, list.get(0).id(), list.get(3).id(), "12345-01")).isEqualTo(2);
        assertThat(blockingStream.xlen(key)).isEqualTo(98);
    }

    @Test
    void xGroupCreateAndDeleteConsumer() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        blockingStream.xgroupCreate(key, "g1", "0");
        assertThat(blockingStream.xgroupCreateConsumer(key, "g1", "c1")).isTrue();
        assertThat(blockingStream.xgroupCreateConsumer(key, "g1", "c2")).isTrue();
        assertThat(blockingStream.xgroupCreateConsumer(key, "g1", "c1")).isFalse();
        assertThatThrownBy(() -> blockingStream.xgroupCreateConsumer(key, "missing", "c3")).hasMessageContaining("missing");

        assertThat(blockingStream.xgroupDelConsumer(key, "g1", "c1")).isEqualTo(0);
        assertThat(blockingStream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);
        assertThat(blockingStream.xgroupDelConsumer(key, "g1", "c2")).isEqualTo(10);
        assertThat(blockingStream.xgroupDestroy(key, "g1")).isTrue();
        assertThat(blockingStream.xgroupDestroy(key, "g1")).isFalse();
    }

    @Test
    void xGroupSetId() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(blockingStream.xadd(key, PAYLOAD));
        }
        blockingStream.xgroupCreate(key, "g1", "0");
        assertThat(blockingStream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);
        blockingStream.xgroupSetId(key, "g1", ids.get(50));
        assertThat(blockingStream.xreadgroup("g1", "c2", key, ">")).hasSize(49);
    }

    @Test
    void xGroupSetIdWithArgs() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(blockingStream.xadd(key, PAYLOAD));
        }
        blockingStream.xgroupCreate(key, "g1", "0", new XGroupCreateArgs().entriesRead("0"));
        assertThat(blockingStream.xreadgroup("g1", "c2", key, ">", new XReadGroupArgs().count(10))).hasSize(10);
        blockingStream.xgroupSetId(key, "g1", ids.get(50), new XGroupSetIdArgs().entriesRead(1234));
        assertThat(blockingStream.xreadgroup("g1", "c2", key, ">")).hasSize(49);
    }

    @Test
    void xPendingSummaryTest() {
        blockingStream.xadd(key, PAYLOAD);
        blockingStream.xtrim(key, new XTrimArgs().maxlen(0));
        blockingStream.xgroupCreate(key, "my-group", "0-0");

        XPendingSummary summaryEmpty = blockingStream.xpending(key, "my-group");
        assertThat(summaryEmpty.getPendingCount()).isEqualTo(0);
        assertThat(summaryEmpty.getHighestId()).isNull();
        assertThat(summaryEmpty.getLowestId()).isNull();
        assertThat(summaryEmpty.getConsumers()).isEmpty();

        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        assertThat(blockingStream.xreadgroup("my-group", "consumer-123", key, ">")).hasSize(100);

        XPendingSummary summary = blockingStream.xpending(key, "my-group");
        assertThat(summary.getPendingCount()).isEqualTo(100L);
        assertThat(summary.getHighestId()).isNotNull();
        assertThat(summary.getLowestId()).isNotNull();
        assertThat(summary.getConsumers()).containsExactly(entry("consumer-123", 100L));
    }

    @Test
    void xPendingSummaryTestWithTwoConsumers() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        blockingStream.xgroupCreate(key, "my-group", "0-0");
        var m1 = blockingStream.xreadgroup("my-group", "consumer-1", key, ">");
        var m2 = blockingStream.xreadgroup("my-group", "consumer-2", key, ">");
        assertThat(m1.size() + m2.size()).isEqualTo(100);

        XPendingSummary summary = blockingStream.xpending(key, "my-group");
        assertThat(summary.getPendingCount()).isEqualTo(100L);
        assertThat(summary.getConsumers()).containsOnlyKeys("consumer-1"); // The second didn't have the chance to poll
    }

    @Test
    void xPendingExtendedTest() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        blockingStream.xgroupCreate(key, "my-group", "0-0");
        assertThat(blockingStream.xreadgroup("my-group", "consumer-123", key, ">")).hasSize(100);

        List<PendingMessage> pending = blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10);
        assertThat(pending).hasSize(10).allSatisfy(msg -> {
            assertThat(msg.getMessageId()).isNotNull();
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery()).isNotNull();
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });
    }

    @Test
    void xPendingExtendedWithConsumerTest() {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        blockingStream.xgroupCreate(key, "my-group", "0-0");
        blockingStream.xreadgroup("my-group", "consumer-123", key, ">");
        blockingStream.xreadgroup("my-group", "consumer-456", key, ">");

        List<PendingMessage> pending = blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().consumer("consumer-123"));
        assertThat(pending).hasSize(10).allSatisfy(msg -> assertThat(msg.getConsumer()).isEqualTo("consumer-123"));
        assertThat(blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().consumer("consumer-456")))
                .isEmpty();
        assertThat(blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().consumer("consumer-missing"))).isEmpty();
    }

    @Test
    void xPendingExtendedTestWithConsumerAndIdle() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        blockingStream.xgroupCreate(key, "my-group", "0-0");
        blockingStream.xreadgroup("my-group", "consumer-123", key, ">");

        assertThat(blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().idle(Duration.ofHours(1)).consumer("consumer-123"))).isEmpty();
        Thread.sleep(20);
        List<PendingMessage> pending = blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().idle(Duration.ofMillis(10)).consumer("consumer-123"));
        assertThat(pending).hasSize(10).allSatisfy(msg -> {
            assertThat(msg.getDeliveryCount()).isEqualTo(1);
            assertThat(msg.getDurationSinceLastDelivery().toMillis()).isGreaterThanOrEqualTo(10);
            assertThat(msg.getConsumer()).isEqualTo("consumer-123");
        });
        assertThat(blockingStream.xpending(key, "my-group", StreamRange.of("-", "+"), 10,
                new XPendingArgs().idle(Duration.ofMillis(10)))).hasSize(10);
    }

    @Test
    void streamWithTypeReference() {
        var stream = blockingDs.stream(new TypeReference<List<Integer>>() {
            // Empty on purpose
        });
        stream.xadd("my-stream", Map.of("duration", List.of(1532), "event-id", List.of(5), "user-id", List.of(77788)));
        stream.xadd("my-stream", Map.of("duration", List.of(1533), "event-id", List.of(6), "user-id", List.of(77788)));
        stream.xadd("my-stream", Map.of("duration", List.of(1534), "event-id", List.of(7), "user-id", List.of(77788)));

        List<StreamMessage<String, String, List<Integer>>> messages = stream.xread("my-stream", "0-0");
        assertThat(messages).hasSize(3).allSatisfy(m -> {
            assertThat(m.key()).isEqualTo("my-stream");
            assertThat(m.id()).isNotEmpty().contains("-");
            assertThat(m.payload()).contains(entry("user-id", List.of(77788))).containsKey("event-id")
                    .containsKey("duration");
        });
    }

    // ------------------------------------------------------------------ helpers

    private void assertMessage(StreamMessage<String, String, Integer> m, List<String> ids) {
        assertThat(m.key()).isEqualTo(key);
        assertThat(m.id()).isNotBlank();
        assertThat(ids).contains(m.id());
        assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
    }

    /** Creates {@code group}, adds 5 entries and delivers {@code count} of them to {@code consumer} without ack. */
    private List<String> readWithoutAck(String group, String consumer, int count) {
        blockingStream.xgroupCreate(key, group, "$", new XGroupCreateArgs().mkstream());
        for (int i = 0; i < 5; i++) {
            blockingStream.xadd(key, PAYLOAD);
        }
        List<String> pending = new ArrayList<>();
        assertThat(blockingStream.xreadgroup(group, consumer, key, ">", new XReadGroupArgs().count(count))).hasSize(count)
                .allSatisfy(m -> {
                    assertThat(m.key()).isEqualTo(key);
                    assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD);
                    pending.add(m.id());
                });
        return pending;
    }

    /** Runs {@code feed} every 10 ms until the blocked reader on the other thread has returned. */
    private static void feedUntil(CountDownLatch latch, Runnable feed) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!latch.await(10, TimeUnit.MILLISECONDS)) {
            assertThat(System.nanoTime()).as("blocked read did not return").isLessThan(deadline);
            feed.run();
        }
    }

    private static boolean redisVersionAtLeast(int major, int minor) {
        String info = connection.sync().info("server");
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("redis_version:")) {
                String[] parts = line.substring("redis_version:".length()).trim().split("\\.");
                int actualMajor = Integer.parseInt(parts[0]);
                int actualMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return actualMajor > major || (actualMajor == major && actualMinor >= minor);
            }
        }
        return false;
    }

    @Test
    void apiReturnsImmutableCollections() {
        String group = "g";
        blockingStream.xgroupCreate(key, group, "$", new XGroupCreateArgs().mkstream());
        blockingStream.xgroupCreate(key, "idle-group", "0");
        blockingStream.xadd(key, PAYLOAD);
        blockingStream.xadd(key, PAYLOAD);

        assertImmutable(blockingStream.xrange(key, StreamRange.of("-", "+")));
        assertImmutable(blockingStream.xrange(key, StreamRange.of("-", "+")).get(0).payload());
        assertImmutable(blockingStream.xrevrange(key, StreamRange.of("+", "-")));
        assertImmutable(blockingStream.xrange("missing", StreamRange.of("-", "+")));
        assertImmutable(blockingStream.xread(key, "0"));
        assertImmutable(blockingStream.xread(Map.of(key, "0"), new XReadArgs().count(1)));

        List<StreamMessage<String, String, Integer>> read = blockingStream.xreadgroup(group, "c1", key, ">");
        assertImmutable(read);
        assertImmutable(read.get(0).payload());

        XPendingSummary summary = blockingStream.xpending(key, group);
        assertImmutable(summary.getConsumers());
        assertImmutable(blockingStream.xpending(key, "idle-group").getConsumers());
        assertImmutable(blockingStream.xpending(key, group, StreamRange.of("-", "+"), 10));
        assertImmutable(blockingStream.xpending(key, group, StreamRange.of("-", "+"), 10, new XPendingArgs().consumer("c1")));

        assertImmutable(blockingStream.xclaim(key, group, "c2", Duration.ZERO, read.get(0).id()));
        ClaimedMessages<String, String, Integer> claimed = blockingStream.xautoclaim(key, group, "c3", Duration.ZERO, "0");
        assertImmutable(claimed.getMessages());
        assertImmutable(claimed.getMessages().get(0).payload());
    }

}

package io.quarkus.redis.lettuce.runtime.internal.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.stream.ReactiveTransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.TransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalStreamCommandsTest extends CommandsTestBase {

    private static final Map<String, Integer> PAYLOAD = Map.of("sensor-id", 1234, "temperature", 19);

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void streamBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalStreamCommands<String, String, Integer> stream = tx.stream(Integer.class);
            assertThat(stream.getDataSource()).isEqualTo(tx);
            stream.xgroupCreate(key, "g1", "$", new XGroupCreateArgs().mkstream());
            stream.xadd(key, PAYLOAD);
            stream.xadd(key, PAYLOAD);
            stream.xlen(key);
            stream.xrange(key, StreamRange.of("-", "+"));
            // inside MULTI a blocking read does not block
            stream.xread(key, "0", new XReadArgs().block(Duration.ofSeconds(10)));
            stream.xreadgroup("g1", "c1", key, ">");
            stream.xpending(key, "g1");
            stream.xdel(key, "0-1");
        });
        assertResult(result);
    }

    @Test
    void streamReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalStreamCommands<String, String, Integer> stream = tx.stream(String.class, String.class,
                    Integer.class);
            return stream.xgroupCreate(key, "g1", "$", new XGroupCreateArgs().mkstream())
                    .chain(() -> stream.xadd(key, PAYLOAD))
                    .chain(() -> stream.xadd(key, PAYLOAD))
                    .chain(() -> stream.xlen(key))
                    .chain(() -> stream.xrange(key, StreamRange.of("-", "+")))
                    .chain(() -> stream.xread(key, "0", new XReadArgs().block(Duration.ofSeconds(10))))
                    .chain(() -> stream.xreadgroup("g1", "c1", key, ">"))
                    .chain(() -> stream.xpending(key, "g1"))
                    .chain(() -> stream.xdel(key, "0-1"));
        }).await().atMost(TIMEOUT);
        assertResult(result);
    }

    private void assertResult(TransactionResult result) {
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(9);
        assertThat((Object) result.get(0)).isNull();
        String id1 = result.get(1);
        String id2 = result.get(2);
        assertThat(id1).isNotBlank();
        assertThat(id2).isNotBlank().isNotEqualTo(id1);
        assertThat((long) result.get(3)).isEqualTo(2L);
        List<StreamMessage<String, String, Integer>> range = result.get(4);
        assertThat(range).extracting(StreamMessage::id).containsExactly(id1, id2);
        assertThat(range).allSatisfy(m -> assertThat(m.payload()).containsExactlyInAnyOrderEntriesOf(PAYLOAD));
        List<StreamMessage<String, String, Integer>> read = result.get(5);
        assertThat(read).extracting(StreamMessage::id).containsExactly(id1, id2);
        List<StreamMessage<String, String, Integer>> group = result.get(6);
        assertThat(group).extracting(StreamMessage::id).containsExactly(id1, id2);
        XPendingSummary pending = result.get(7);
        assertThat(pending.getPendingCount()).isEqualTo(2L);
        assertThat(pending.getConsumers()).containsEntry("c1", 2L);
        assertThat((int) result.get(8)).isZero();
    }

}

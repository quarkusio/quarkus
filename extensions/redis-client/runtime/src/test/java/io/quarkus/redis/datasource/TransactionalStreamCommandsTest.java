package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.TransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings("unchecked")
@RequiresRedis6OrHigher
public class TransactionalStreamCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    public static final Map<String, String> payload = Map.of("message", "hello");

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void streamBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalStreamCommands<String, String, String> stream = tx.stream(String.class);
            assertThat(stream.getDataSource()).isEqualTo(tx);

            stream.xadd(key, payload);
            stream.xadd(key, new XAddArgs().nomkstream(), payload);

            stream.xread(key, "0"); // 3 -> 2 messages
            stream.xgroupCreate(key, "g1", "0");
            stream.xreadgroup("g1", "c1", key, ">");
            stream.xpending(key, "g1");
            stream.xpending(key, "g1", StreamRange.of("-", "+"), 10, new XPendingArgs().consumer("c1"));

        });
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat((String) result.get(0)).isNotBlank();
        assertThat((String) result.get(1)).isNotBlank();

        String id1 = result.get(0);
        String id2 = result.get(1);

        assertThat((List<StreamMessage<String, String, String>>) result.get(2)).hasSize(2);
        assertThat((List<StreamMessage<String, String, String>>) result.get(4)).hasSize(2);

        assertThat(((XPendingSummary) result.get(5)).getPendingCount()).isEqualTo(2);
        List<PendingMessage> list = result.get(6);

        assertThat(((List<PendingMessage>) result.get(6))).hasSize(2);
        List<String> ids = list.stream().map(PendingMessage::getMessageId).collect(Collectors.toList());
        assertThat(ids).containsExactly(id1, id2);
    }

    @Test
    public void streamReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            var stream = tx.stream(String.class);
            assertThat(stream.getDataSource()).isEqualTo(tx);

            return stream.xadd(key, payload)
                    .chain((x) -> stream.xadd(key, new XAddArgs().nomkstream(), payload))
                    .chain(x -> stream.xread(key, "0"))
                    .chain(x -> stream.xgroupCreate(key, "g1", "0"))
                    .chain(x -> stream.xreadgroup("g1", "c1", key, ">"))
                    .chain(x -> stream.xpending(key, "g1"))
                    .chain(x -> stream.xpending(key, "g1", StreamRange.of("-", "+"), 10));

        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(7);
        assertThat(result.discarded()).isFalse();
        assertThat((String) result.get(0)).isNotBlank();
        assertThat((String) result.get(1)).isNotBlank();

        String id1 = result.get(0);
        String id2 = result.get(1);

        assertThat((List<StreamMessage<String, String, String>>) result.get(2)).hasSize(2);
        assertThat((List<StreamMessage<String, String, String>>) result.get(4)).hasSize(2);

        assertThat(((XPendingSummary) result.get(5)).getPendingCount()).isEqualTo(2);
        List<PendingMessage> list = result.get(6);

        assertThat(((List<PendingMessage>) result.get(6))).hasSize(2);
        List<String> ids = list.stream().map(PendingMessage::getMessageId).collect(Collectors.toList());
        assertThat(ids).containsExactly(id1, id2);
    }

}

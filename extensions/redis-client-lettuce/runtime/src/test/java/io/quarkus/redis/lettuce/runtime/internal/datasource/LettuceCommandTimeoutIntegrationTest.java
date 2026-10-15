package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandTimeoutException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionFactory;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;

/**
 * Exercises the client-side command timeout that {@link LettuceConnectionFactory} configures. The
 * shared test client in {@link CommandsTestBase} is created without it, so these tests build their
 * own client through the factory with a short timeout.
 */
class LettuceCommandTimeoutIntegrationTest extends CommandsTestBase {

    private static final Duration COMMAND_TIMEOUT = Duration.ofMillis(500);

    private LettuceConnectionFactory factory;
    private LettuceConnection conn;
    private LettuceConnectionPool pool;
    private RedisDataSource ds;

    @BeforeEach
    void connectWithShortCommandTimeout() {
        factory = new LettuceConnectionFactory("timeout-test", lettuceResources.clientResources(),
                "redis://" + REDIS.getHost() + ":" + REDIS.getFirstMappedPort(), COMMAND_TIMEOUT);
        conn = factory.connect();
        pool = new LettuceConnectionPool(factory::connectAsync, 1, 1, 0);
        ds = new LettuceBlockingRedisDataSourceImpl(
                new LettuceReactiveRedisDataSourceImpl(vertx, conn, pool, factory::connectPubSubAsync), TIMEOUT);
    }

    @AfterEach
    void shutdown() {
        pool.close().await().atMost(TIMEOUT);
        conn.close();
        factory.shutdown();
    }

    @Test
    void transactionOutlivingTheCommandTimeoutStillSucceeds() {
        // Commands queued after MULTI get their reply only with EXEC. A client-side timer on them
        // would expire during a slow block and report errors for commands Redis then executed.
        TransactionResult result = ds.withTransaction(tx -> {
            var value = tx.value(String.class, String.class);
            value.set(key, "v");
            value.get(key);
            sleep(COMMAND_TIMEOUT.multipliedBy(2));
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((String) result.get(1)).isEqualTo("v");
        assertThat(rawGet(key)).isEqualTo("v");
    }

    @Test
    void ordinaryCommandStillTimesOutClientSide() {
        // WAIT for a replica that does not exist never returns. The exemption above must not have
        // widened: this must fail with the 500ms client-side timeout, well before the blocking
        // API's own 5s bound.
        assertThatThrownBy(() -> ds.execute("WAIT", "1", "0"))
                .isInstanceOf(RedisCommandTimeoutException.class);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

}

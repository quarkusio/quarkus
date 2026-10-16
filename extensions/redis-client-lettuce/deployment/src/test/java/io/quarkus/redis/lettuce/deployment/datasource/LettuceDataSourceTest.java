package io.quarkus.redis.lettuce.deployment.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Response;

/**
 * Integration test for the Lettuce-backed {@link RedisDataSource} / {@link ReactiveRedisDataSource}.
 * <p>
 * Verifies that adding the Lettuce extension produces synthetic data source beans
 * that route through the Lettuce backend, that {@code execute(...)} works, and that the methods
 * which expose Vert.x-specific types throw {@link UnsupportedOperationException} with the
 * documented adoption-friendly message.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource blocking;

    @Inject
    ReactiveRedisDataSource reactive;

    @Inject
    StatefulRedisConnection<String, String> sharedConnection;

    @Inject
    Instance<StatefulRedisConnection<String, String>> connectionInstance;

    @Test
    public void testBeansAreInjected() {
        assertThat(blocking).isNotNull();
        assertThat(reactive).isNotNull();
        assertThat(blocking.getReactive()).isNotNull();
    }

    @Test
    public void testExecuteString() {
        Response response = blocking.execute("PING");
        assertThat(response).isNotNull();
        assertThat(response.toString()).isEqualTo("PONG");
    }

    @Test
    public void testExecuteCommand() {
        Response response = blocking.execute(Command.PING);
        assertThat(response).isNotNull();
        assertThat(response.toString()).isEqualTo("PONG");
    }

    @Test
    public void testExecuteWithArgs() {
        blocking.execute("SET", "lettuce:exec:k1", "v1");
        Response value = blocking.execute("GET", "lettuce:exec:k1");
        assertThat(value).isNotNull();
        assertThat(value.toString()).isEqualTo("v1");
    }

    @Test
    public void testExecuteReactive() {
        Response response = reactive.execute("PING").await().indefinitely();
        assertThat(response).isNotNull();
        assertThat(response.toString()).isEqualTo("PONG");
    }

    @Test
    public void testValueCommandsRouteThroughLettuce() {
        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("lettuce:ds:value", "v");
        assertThat(values.get("lettuce:ds:value")).isEqualTo("v");
    }

    @Test
    public void testFlushAll() {
        blocking.value(String.class).set("lettuce:ds:flush", "v");
        blocking.flushall();
        assertThat(blocking.value(String.class).get("lettuce:ds:flush")).isNull();
    }

    @Test
    public void testGetRedisThrowsUnsupported() {
        assertThatThrownBy(() -> reactive.getRedis())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Lettuce backend")
                .hasMessageContaining("execute")
                .hasMessageContaining("quarkus-redis-client-lettuce");
    }

    @Test
    public void testWithConnection() {
        blocking.withConnection(ds -> {
            ds.value(String.class).set("lettuce:ds:withconn", "v1");
            assertThat(ds.value(String.class).get("lettuce:ds:withconn")).isEqualTo("v1");
        });
        // Writes made on the dedicated connection are visible on the shared one afterwards.
        assertThat(blocking.value(String.class).get("lettuce:ds:withconn")).isEqualTo("v1");
    }

    @Test
    public void testWithTransaction() {
        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set("lettuce:ds:tx1", "a");
            tx.value(String.class).set("lettuce:ds:tx2", "b");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        // Both queued commands were applied atomically on EXEC.
        assertThat(blocking.value(String.class).get("lettuce:ds:tx1")).isEqualTo("a");
        assertThat(blocking.value(String.class).get("lettuce:ds:tx2")).isEqualTo("b");
    }

    @Test
    public void testSelectChangesDatabase() {
        ValueCommands<String, String> values = blocking.value(String.class);
        try {
            values.set("lettuce:select:k", "db0");
            blocking.select(1);
            assertThat(values.get("lettuce:select:k")).isNull();
            values.set("lettuce:select:k", "db1");
            assertThat(values.get("lettuce:select:k")).isEqualTo("db1");
            blocking.select(0);
            assertThat(values.get("lettuce:select:k")).isEqualTo("db0");
        } finally {
            blocking.select(1);
            blocking.flushall();
            blocking.select(0);
            blocking.flushall();
        }
    }

    @Test
    public void testConnectionIsCachedAndStaysOpen() {
        assertThat(sharedConnection.isOpen()).isTrue();
        // Synthetic bean is @ApplicationScoped over a supplier that caches the connection
        // in LettuceRecorder; every lookup must return the same proxy.
        StatefulRedisConnection<String, String> first = connectionInstance.get();
        StatefulRedisConnection<String, String> second = connectionInstance.get();
        assertThat(first).isSameAs(second);

        ValueCommands<String, String> values = blocking.value(String.class);
        for (int i = 0; i < 50; i++) {
            values.set("lettuce:lifecycle:" + i, Integer.toString(i));
        }
        assertThat(sharedConnection.isOpen()).isTrue();
        assertThat(connectionInstance.get()).isSameAs(first);
    }
}

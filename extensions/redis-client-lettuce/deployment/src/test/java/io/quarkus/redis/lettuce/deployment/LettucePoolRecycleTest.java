package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.redis.client.Command;

/**
 * Verifies that {@code pool-recycle-timeout} and {@code pool-cleaner-interval} apply to the pool the Lettuce backend
 * uses for blocking commands and scoped connections: a pooled connection left idle for longer than the recycle
 * timeout is closed by the cleaner, observed through {@code CLIENT LIST}, while a connection in use is left alone
 * however long it is held. The connections are named so that only this client's are counted.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettucePoolRecycleTest {

    private static final Duration RECYCLE_TIMEOUT = Duration.ofSeconds(1);

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.configure-client-name", "true")
            .overrideConfigKey("quarkus.redis.client-name", "recycle")
            .overrideConfigKey("quarkus.redis.pool-recycle-timeout", RECYCLE_TIMEOUT.toMillis() + "ms")
            .overrideConfigKey("quarkus.redis.pool-cleaner-interval", "200ms");

    @Inject
    RedisDataSource ds;

    @Test
    void idlePooledConnectionIsClosedAfterTheRecycleTimeout() {
        // only the shared connection of the data source, once a pooled connection of the other test is recycled
        await().atMost(Duration.ofSeconds(5)).until(() -> namedConnections() == 1);

        // a blocking command borrows a pooled connection and releases it, idle
        ds.list(String.class).blpop(Duration.ofMillis(100), "recycle-" + UUID.randomUUID());
        assertThat(namedConnections()).isEqualTo(2);

        await().atMost(Duration.ofSeconds(5)).until(() -> namedConnections() == 1);
    }

    @Test
    void connectionInUseIsNotRecycled() {
        ds.withConnection(pinned -> {
            try {
                // several cleaner runs past the recycle timeout while the connection is checked out
                Thread.sleep(RECYCLE_TIMEOUT.plusMillis(500).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            assertThat(pinned.execute(Command.PING).toString()).isEqualTo("PONG");
        });
    }

    /** The connections of this client, which {@code configure-client-name} names {@code recycle}. */
    private long namedConnections() {
        return ds.execute(Command.CLIENT, "LIST").toString().lines()
                .filter(line -> line.contains(" name=recycle "))
                .count();
    }
}

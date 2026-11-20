package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisException;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code max-waiting-handlers} bounds the commands in flight on the shared connection of a data source:
 * while the server holds its replies back, the commands beyond the bound fail at once instead of queueing, and the
 * ones within it complete when the server answers again.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceRequestQueueBoundTest {

    private static final int BOUND = 4;

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.max-waiting-handlers", String.valueOf(BOUND));

    @Inject
    RedisDataSource ds;

    @Inject
    ReactiveRedisDataSource reactive;

    @Test
    void commandsBeyondTheBoundFailWhileTheServerHoldsItsReplies() {
        ds.value(String.class).set("queue-bound", "value");
        // the server answers no client for a second: every command sent meanwhile stays in flight
        assertThat(ds.execute("CLIENT", "PAUSE", "1000").toString()).isEqualTo("OK");

        List<CompletableFuture<String>> commands = new ArrayList<>();
        for (int i = 0; i < 4 * BOUND; i++) {
            commands.add(reactive.value(String.class).get("queue-bound").subscribeAsCompletionStage());
        }

        List<Throwable> rejected = new ArrayList<>();
        int completed = 0;
        for (CompletableFuture<String> command : commands) {
            try {
                assertThat(command.orTimeout(10, TimeUnit.SECONDS).join()).isEqualTo("value");
                completed++;
            } catch (CompletionException e) {
                rejected.add(e.getCause());
            }
        }
        assertThat(completed).as("commands within the bound").isBetween(1, BOUND);
        assertThat(rejected).as("commands beyond the bound").hasSize(4 * BOUND - completed)
                .allSatisfy(failure -> assertThat(failure).isInstanceOf(RedisException.class)
                        .hasMessageContaining("size exceeded"));
        // the connection is usable afterwards
        assertThat(reactive.value(String.class).get("queue-bound").await().atMost(Duration.ofSeconds(10)))
                .isEqualTo("value");
    }
}

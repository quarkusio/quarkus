
package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

@RequiresRedis6OrHigher
public class TransactionInternalErrorHandlingTest extends DatasourceTestBase {
    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    // we simulate a situation where `MULTI` requests do reach the server, but the responses don't reach the client
    // all other request/response cycles are left intact

    @BeforeEach
    void initialize() {
        Redis specialRedis = new Redis(redis.getDelegate()) {
            @Override
            public Uni<RedisConnection> connect() {
                return super.connect().map(conn -> {
                    return new RedisConnection(conn.getDelegate()) {
                        @Override
                        public Uni<Response> send(Request command) {
                            Uni<Response> result = super.send(command);
                            if (command.command().equals(Command.MULTI)) {
                                return result.replaceWith(Uni.createFrom().failure(new IOException("intentional")));
                            }
                            return result;
                        }
                    };
                });
            }
        };

        blocking = new BlockingRedisDataSourceImpl(vertx, specialRedis, api, Duration.ofSeconds(10));
        reactive = new ReactiveRedisDataSourceImpl(vertx, specialRedis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void blockingTx() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
            });
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithWatch() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithOptimisticLocking() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                tx.value(String.class).set(key, input + "|foobar");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    // ---

    @Test
    public void reactiveTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar");
            }).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithWatch() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        // `WATCH` is fine, but the subsequent `MULTI` fails
        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar");
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithOptimisticLocking() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        // `WATCH` is fine, but the subsequent `MULTI` fails
        assertThatThrownBy(() -> {
            reactive.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                return tx.value(String.class).set(key, input + "|foobar");
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IOException.class)
                .hasMessageContaining("intentional");

        assertNoClientInTransaction();
        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    // ---

    private void assertNoClientInTransaction() {
        blocking.execute(Command.CLIENT, "LIST").toString().lines().forEach(line -> {
            Map<String, String> info = parseClientLine(line);
            assertThat(info.getOrDefault("multi", "")).isEqualTo("-1");
            assertThat(info.getOrDefault("flags", "")).doesNotContain("x");
        });
    }

    private Map<String, String> parseClientLine(String line) {
        Map<String, String> result = new HashMap<>();
        for (String element : line.split("\\s+")) {
            String[] parts = element.split("=", 2);
            result.put(parts[0], parts[1]);
        }
        return result;
    }
}

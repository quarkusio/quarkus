package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisConnectionException;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.lettuce.runtime.internal.LettuceSharedConnection;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * The data sources on the shared connection of a client as the extension creates it: opened by the first command
 * instead of at startup. The commands sent before it is established run in the order they were sent, a connection
 * that cannot be opened fails the commands waiting for it and the next one tries again, and what does not use the
 * shared connection (scoped connections, transactions, blocking commands) does not open it.
 */
class LettuceLazyConnectionIntegrationTest extends CommandsTestBase {

    private final AtomicInteger attempts = new AtomicInteger();
    private final List<LettuceSharedConnection> shared = new ArrayList<>();
    private final List<LettuceConnectionPool> pools = new ArrayList<>();

    private LettuceReactiveRedisDataSourceImpl dataSource(Supplier<CompletionStage<LettuceConnection>> connector) {
        LettuceSharedConnection connection = new LettuceSharedConnection(() -> {
            attempts.incrementAndGet();
            return connector.get();
        }, false);
        LettuceConnectionPool pool = pool();
        shared.add(connection);
        pools.add(pool);
        return new LettuceReactiveRedisDataSourceImpl(vertx, connection, pool, pubSubConnector());
    }

    @AfterEach
    void close() {
        shared.forEach(LettuceSharedConnection::close);
        pools.forEach(pool -> pool.close().await().atMost(TIMEOUT));
    }

    @Test
    void theFirstCommandOpensTheConnectionAndTheNextOnesReuseIt() {
        long before = connectionCount();
        LettuceReactiveRedisDataSourceImpl ds = dataSource(connector());
        ReactiveValueCommands<String, String> values = ds.value(String.class, String.class);
        ds.key(String.class);

        assertThat(attempts).hasValue(0);
        assertThat(connectionCount()).isEqualTo(before);

        values.set(key, "hello").await().atMost(TIMEOUT);
        assertThat(attempts).hasValue(1);
        assertThat(connectionCount()).isEqualTo(before + 1);

        assertThat(values.get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertThat(ds.execute("PING").await().atMost(TIMEOUT).toString()).isEqualTo("PONG");
        assertThat(ds.key(String.class).scan().toMulti().collect().asList().await().atMost(TIMEOUT)).containsExactly(key);
        assertThat(attempts).hasValue(1);
        assertThat(connectionCount()).isEqualTo(before + 1);
    }

    @Test
    void commandsSentBeforeTheConnectionIsEstablishedRunInTheOrderTheyWereSent() {
        CompletableFuture<Void> serverReachable = new CompletableFuture<>();
        LettuceReactiveRedisDataSourceImpl ds = dataSource(() -> serverReachable.thenCompose(ignored -> connectAsync()));
        ReactiveValueCommands<String, String> values = ds.value(String.class, String.class);

        int commands = 200;
        List<UniAssertSubscriber<Long>> appended = new ArrayList<>();
        for (int i = 0; i < commands; i++) {
            appended.add(values.append(key, i + ",").subscribe().withSubscriber(UniAssertSubscriber.create()));
        }
        assertThat(attempts).hasValue(1);
        appended.forEach(UniAssertSubscriber::assertNotTerminated);

        serverReachable.complete(null);

        appended.forEach(subscriber -> subscriber.awaitItem(TIMEOUT));
        String expected = IntStream.range(0, commands).mapToObj(i -> i + ",").collect(Collectors.joining());
        assertThat(rawGet(key)).isEqualTo(expected);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void aConnectionThatCannotBeOpenedFailsTheCommandAndTheNextOneTriesAgain() {
        LettuceReactiveRedisDataSourceImpl ds = dataSource(() -> attempts.get() == 1
                ? CompletableFuture.failedFuture(new RedisConnectionException("Unable to connect"))
                : connectAsync());
        ReactiveValueCommands<String, String> values = ds.value(String.class, String.class);

        assertThatThrownBy(() -> values.get(key).await().atMost(TIMEOUT)).isInstanceOf(RedisConnectionException.class);
        // an argument that fails its validation does not need the connection, and does not ask for one
        assertThatThrownBy(() -> ds.countmin(String.class, String.class).cmsIncrBy(key, java.util.Map.of())
                .await().atMost(TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts).hasValue(1);

        rawSet(key, "hello");
        assertThat(values.get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void scopedConnectionsTransactionsAndBlockingCommandsDoNotOpenTheSharedConnection() {
        LettuceReactiveRedisDataSourceImpl reactive = dataSource(connector());
        LettuceBlockingRedisDataSourceImpl ds = new LettuceBlockingRedisDataSourceImpl(reactive, TIMEOUT);

        TransactionResult result = ds.withTransaction(tx -> tx.value(String.class).set(key, "tx"));
        assertThat(result.discarded()).isFalse();
        ds.withConnection(scoped -> scoped.value(String.class).append(key, "+scoped"));
        assertThat(ds.list(String.class).blpop(Duration.ofMillis(100), key + "-list")).isNull();

        assertThat(rawGet(key)).isEqualTo("tx+scoped");
        assertThat(attempts).hasValue(0);

        assertThat(ds.value(String.class).get(key)).isEqualTo("tx+scoped");
        assertThat(attempts).hasValue(1);
    }

    @Test
    void theCommandOpeningTheConnectionCompletesOnTheCallersContext() throws Exception {
        LettuceReactiveRedisDataSourceImpl ds = dataSource(connector());
        rawSet(key, "hello");
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        CompletableFuture<Context> emittedOn = new CompletableFuture<>();

        context.runOnContext(ignored -> ds.value(String.class, String.class).get(key).subscribe().with(
                item -> emittedOn.complete(Vertx.currentContext()), emittedOn::completeExceptionally));

        assertThat(emittedOn.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isSameAs(context);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void aClosedSharedConnectionFailsTheCommands() {
        LettuceReactiveRedisDataSourceImpl ds = dataSource(connector());
        ds.value(String.class, String.class).set(key, "hello").await().atMost(TIMEOUT);
        long open = connectionCount();

        shared.get(0).close();

        assertThat(connectionCount()).isEqualTo(open - 1);
        assertThatThrownBy(() -> ds.value(String.class, String.class).get(key).await().atMost(TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

}

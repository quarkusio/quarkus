package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;

/**
 * The transaction paths must not leave a pooled connection in a transaction, or watching keys, when something goes
 * wrong: the reply to {@code MULTI} fails or never arrives although the server entered the transaction, or the user
 * block throws an {@link Error}. The port of upstream's {@code TransactionInternalErrorHandlingTest}, which asserts
 * through {@code CLIENT LIST} that no client stays in a transaction; here the pool must also keep serving.
 */
class LettuceTransactionInternalErrorHandlingTest extends CommandsTestBase {

    /** How the proxied connection answers {@code MULTI}: with a failure once the real reply is in, or never. */
    private enum MultiReply {
        FAIL,
        NEVER
    }

    /**
     * A connection whose {@code MULTI} reaches the server but whose reply is replaced as {@code reply} says; every
     * other command passes through. A JDK proxy of the Lettuce connection and of its asynchronous commands.
     */
    private static LettuceConnection withMultiReply(LettuceConnection real, MultiReply reply) {
        StatefulRedisConnection<byte[], byte[]> connection = (StatefulRedisConnection<byte[], byte[]>) real.connection();
        RedisAsyncCommands<byte[], byte[]> async = connection.async();
        RedisAsyncCommands<byte[], byte[]> commands = (RedisAsyncCommands<byte[], byte[]>) Proxy.newProxyInstance(
                RedisAsyncCommands.class.getClassLoader(), new Class<?>[] { RedisAsyncCommands.class },
                (proxy, method, args) -> {
                    Object result = invoke(method, async, args);
                    if (!method.getName().equals("multi")) {
                        return result;
                    }
                    FakeRedisFuture replaced = new FakeRedisFuture();
                    if (reply == MultiReply.FAIL) {
                        ((RedisFuture<?>) result).whenComplete(
                                (r, f) -> replaced.completeExceptionally(new IOException("intentional")));
                    }
                    return replaced;
                });
        StatefulRedisConnection<byte[], byte[]> proxied = (StatefulRedisConnection<byte[], byte[]>) Proxy
                .newProxyInstance(StatefulRedisConnection.class.getClassLoader(),
                        new Class<?>[] { StatefulRedisConnection.class },
                        (proxy, method, args) -> method.getName().equals("async") ? commands
                                : invoke(method, connection, args));
        return LettuceConnection.standalone(proxied);
    }

    private static Object invoke(java.lang.reflect.Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** A {@link RedisFuture} the test completes itself. */
    private static final class FakeRedisFuture extends CompletableFuture<String> implements RedisFuture<String> {

        @Override
        public String getError() {
            return null;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            try {
                get(timeout, unit);
            } catch (ExecutionException | TimeoutException e) {
                // the outcome is what the caller reads from the future
            }
            return isDone();
        }
    }

    private static LettuceConnectionPool poolWith(MultiReply reply) {
        return new LettuceConnectionPool(() -> connectAsync().thenApply(conn -> withMultiReply(conn, reply)), 1, 1,
                redisUri.getDatabase());
    }

    private static LettuceReactiveRedisDataSourceImpl reactive(MultiReply reply) {
        return new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, poolWith(reply), pubSubConnector());
    }

    private static RedisDataSource blocking(MultiReply reply, Duration timeout) {
        return new LettuceBlockingRedisDataSourceImpl(reactive(reply), timeout);
    }

    @Test
    void blockingTxWithFailingMulti() {
        RedisDataSource ds = blocking(MultiReply.FAIL, TIMEOUT);
        ds.value(String.class, String.class).set(key, "hello");
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "foobar")))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void blockingTxWithWatchAndFailingMulti() {
        RedisDataSource ds = blocking(MultiReply.FAIL, TIMEOUT);
        ds.value(String.class, String.class).set(key, "hello");
        assertThatThrownBy(
                () -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "foobar"), key))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void blockingTxWithOptimisticLockingAndFailingMulti() {
        RedisDataSource ds = blocking(MultiReply.FAIL, TIMEOUT);
        ds.value(String.class, String.class).set(key, "hello");
        assertThatThrownBy(() -> ds.withTransaction(preTx -> preTx.value(String.class, String.class).get(key),
                (input, tx) -> tx.value(String.class, String.class).set(key, input + "|foobar"), key))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void blockingTxWithMultiTimingOut() {
        // the reply never arrives: the blocking API gives up after its timeout, and the transaction the server
        // entered is discarded so that the connection does not queue every later command
        RedisDataSource ds = blocking(MultiReply.NEVER, Duration.ofMillis(500));
        ds.value(String.class, String.class).set(key, "hello");
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "foobar")))
                .isInstanceOf(io.smallrye.mutiny.TimeoutException.class);
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void blockingTxWithAnErrorInTheBlock() {
        RedisDataSource ds = blockingDataSource();
        ds.value(String.class, String.class).set(key, "hello");
        assertThatThrownBy(() -> ds.withTransaction(tx -> {
            tx.value(String.class, String.class).set(key, "foobar");
            throw new AssertionError("boom");
        })).isInstanceOf(AssertionError.class).hasMessage("boom");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void reactiveTxWithFailingMulti() {
        ReactiveRedisDataSource ds = reactive(MultiReply.FAIL);
        ds.value(String.class, String.class).set(key, "hello").await().atMost(TIMEOUT);
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "foobar"))
                .await().atMost(TIMEOUT))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void reactiveTxWithWatchAndFailingMulti() {
        ReactiveRedisDataSource ds = reactive(MultiReply.FAIL);
        ds.value(String.class, String.class).set(key, "hello").await().atMost(TIMEOUT);
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "foobar"), key)
                .await().atMost(TIMEOUT))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void reactiveTxWithOptimisticLockingAndFailingMulti() {
        ReactiveRedisDataSource ds = reactive(MultiReply.FAIL);
        ds.value(String.class, String.class).set(key, "hello").await().atMost(TIMEOUT);
        assertThatThrownBy(() -> ds.withTransaction(preTx -> preTx.value(String.class, String.class).get(key),
                (input, tx) -> tx.value(String.class, String.class).set(key, input + "|foobar"), key)
                .await().atMost(TIMEOUT))
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("intentional");
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    @Test
    void reactiveTxWithAnErrorInTheBlock() {
        ReactiveRedisDataSource ds = reactiveDataSource();
        ds.value(String.class, String.class).set(key, "hello").await().atMost(TIMEOUT);
        assertThatThrownBy(() -> ds.withTransaction(tx -> {
            throw new AssertionError("boom");
        }).await().atMost(TIMEOUT))
                .satisfies(t -> assertThat(t instanceof AssertionError ? t : t.getCause())
                        .isInstanceOf(AssertionError.class).hasMessage("boom"));
        assertNoClientInTransaction();
        assertThat(ds.value(String.class, String.class).get(key).await().atMost(TIMEOUT)).isEqualTo("hello");
        assertPoolServes(ds);
    }

    /** The pool still serves after the failed transaction: its connection was discarded, or closed and replaced. */
    private void assertPoolServes(RedisDataSource ds) {
        ds.withConnection(scoped -> assertThat(scoped.value(String.class, String.class).get(key)).isEqualTo("hello"));
    }

    private void assertPoolServes(ReactiveRedisDataSource ds) {
        ds.withConnection(scoped -> scoped.value(String.class, String.class).get(key)
                .invoke(value -> assertThat(value).isEqualTo("hello"))
                .replaceWithVoid()).await().atMost(TIMEOUT);
    }

}

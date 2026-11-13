package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

/**
 * A cancelled {@code withTransaction} or {@code withConnection} does not return its connection to the pool. The steps
 * of a transaction undo themselves when they fail, but nothing runs after a cancellation, and a cancelled command may
 * or may not have reached the server: the connection may be watching keys, be in a transaction Lettuce no longer
 * knows about, or have a blocking command pending. Handed to the next borrower, it would abort that borrower's
 * transaction, answer {@code QUEUED} to its commands, or keep them waiting. So the connection is closed, which
 * {@code CLIENT LIST} shows, and the pool opens a fresh one.
 */
class LettuceScopedCancellationTest extends CommandsTestBase {

    private final LettuceConnectionPool pool = pool(1, 1);
    private final LettuceReactiveRedisDataSourceImpl ds = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
            pool, pubSubConnector());
    private final String blocker = "blocker-" + UUID.randomUUID();
    /** The id the server gave to the pooled connection of the scope under test. */
    private final CompletableFuture<Long> pooledClient = new CompletableFuture<>();

    @AfterEach
    void closePool() {
        pool.close().await().atMost(TIMEOUT);
    }

    @Test
    void aTransactionCancelledWhileWatchingDoesNotReturnItsConnectionToThePool() throws Exception {
        Cancellable transaction = ds.<String> withTransaction(
                // WATCH is in place, MULTI is not sent: the pre-transaction block never completes
                preTx -> reportClient(preTx).chain(() -> Uni.createFrom().<String> nothing()),
                (input, tx) -> tx.value(String.class, String.class).set(key, "never"), key)
                .subscribe().with(x -> {
                }, t -> {
                });
        long id = pooledClientId();
        assertThat(clientInfo(id)).containsEntry("watch", "1");

        transaction.cancel();

        await().atMost(TIMEOUT).until(() -> clientInfo(id) == null);
        assertThat(pool.getObjectCount()).isZero();
        // A reused connection would still be watching the key: modified now, it would abort the next transaction.
        rawSet(key, "changed");
        OptimisticLockingTransactionResult<String> next = ds.withTransaction(
                preTx -> preTx.value(String.class, String.class).get(key),
                (input, tx) -> tx.value(String.class, String.class).set(key, input + "|committed"), key)
                .await().atMost(TIMEOUT);
        assertThat(next.discarded()).isFalse();
        assertThat(rawGet(key)).isEqualTo("changed|committed");
    }

    @Test
    void aTransactionCancelledWithMultiInFlightDoesNotReturnItsConnectionToThePool() throws Exception {
        Cancellable transaction = ds.<String> withTransaction(
                // An unanswered BLPOP holds the connection on the server: the MULTI sent next is not processed, and
                // not answered, before the BLPOP returns.
                preTx -> reportClient(preTx)
                        .invoke(() -> preTx.list(String.class, String.class).blpop(Duration.ofSeconds(30), blocker)
                                .subscribe().with(x -> {
                                }, t -> {
                                }))
                        .replaceWith("input"),
                (input, tx) -> tx.value(String.class, String.class).set(key, "never"), key)
                .subscribe().with(x -> {
                }, t -> {
                });
        long id = pooledClientId();
        // MULTI is on its way: sent, and waiting in the query buffer of the blocked client
        await().atMost(TIMEOUT).until(() -> {
            Map<String, String> info = clientInfo(id);
            return info != null && info.get("flags").contains("b") && !"0".equals(info.get("qbuf"));
        });

        // Once the transaction is cancelled, Lettuce no longer knows about the MULTI it sent (it forgets a transaction
        // whose MULTI reply it stops waiting for), although the server enters the transaction as soon as it gets to
        // it: asking Lettuce would not keep this connection out of the pool, closing it does.
        transaction.cancel();
        rawPush(blocker, "unblocked");

        await().atMost(TIMEOUT).until(() -> clientInfo(id) == null);
        assertNoClientInTransaction();
        assertThat(pool.getObjectCount()).isZero();
    }

    @Test
    void aTransactionCancelledInItsBlockDoesNotReturnItsConnectionToThePool() throws Exception {
        CompletableFuture<Void> inBlock = new CompletableFuture<>();
        Cancellable transaction = ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "never")
                .invoke(() -> inBlock.complete(null))
                .chain(() -> Uni.createFrom().<Void> nothing()))
                .subscribe().with(x -> {
                }, t -> {
                });
        inBlock.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        transaction.cancel();

        await().atMost(TIMEOUT).untilAsserted(CommandsTestBase::assertNoClientInTransaction);
        assertThat(pool.getObjectCount()).isZero();
        assertThat(rawGet(key)).isNull();
    }

    @Test
    void aConnectionScopeCancelledWithACommandBlockedDoesNotReturnItsConnectionToThePool() throws Exception {
        Cancellable scope = ds.withConnection(pinned -> reportClient(pinned)
                .chain(() -> pinned.list(String.class, String.class).blpop(Duration.ofSeconds(30), blocker))
                .replaceWithVoid())
                .subscribe().with(x -> {
                }, t -> {
                });
        long id = pooledClientId();
        await().atMost(TIMEOUT).until(() -> {
            Map<String, String> info = clientInfo(id);
            return info != null && info.get("flags").contains("b");
        });

        scope.cancel();

        // The server would keep the BLPOP of a reused connection pending, in front of the next borrower's commands,
        // and give it the next element pushed, which nobody would get.
        await().atMost(TIMEOUT).until(() -> clientInfo(id) == null);
        rawPush(blocker, "kept");
        assertThat(connection.sync().llen(blocker.getBytes(StandardCharsets.UTF_8))).isEqualTo(1);
        ds.withConnection(pinned -> pinned.value(String.class, String.class).set(key, "served"))
                .await().atMost(Duration.ofSeconds(1));
        assertThat(rawGet(key)).isEqualTo("served");
    }

    @Test
    void aTransactionCancelledInsideAConnectionScopeMakesTheScopeCloseItsConnection() throws Exception {
        ds.withConnection(pinned -> reportClient(pinned)
                .chain(() -> pinned.<String> withTransaction(preTx -> Uni.createFrom().<String> nothing(),
                        (input, tx) -> tx.value(String.class, String.class).set(key, "never"), key)
                        // gives up on the transaction, not on the scope
                        .ifNoItem().after(Duration.ofMillis(200)).fail()
                        .onFailure().recoverWithNull())
                .replaceWithVoid())
                .await().atMost(TIMEOUT);

        // The scope ended normally, but the transaction it gave up on left its connection watching the key.
        long id = pooledClientId();
        await().atMost(TIMEOUT).until(() -> clientInfo(id) == null);
        assertThat(pool.getObjectCount()).isZero();
    }

    /** Records which client of the server the pinned connection of {@code pinned} is. */
    private Uni<Void> reportClient(ReactiveRedisDataSource pinned) {
        return pinned.execute("CLIENT", "ID").invoke(id -> pooledClient.complete(id.toLong())).replaceWithVoid();
    }

    private long pooledClientId() throws Exception {
        return pooledClient.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private static void rawPush(String key, String value) {
        connection.sync().rpush(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }

}

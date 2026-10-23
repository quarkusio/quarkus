package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

class LettuceConnectionPoolTest extends CommandsTestBase {

    @Test
    void acquireReleaseReusesConnection() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection first = pool.acquireBlocking(TIMEOUT);
        long firstId = sync(first).clientId();
        pool.release(first).await().atMost(TIMEOUT);

        LettuceConnection second = pool.acquireBlocking(TIMEOUT);
        assertThat(sync(second).clientId()).isEqualTo(firstId);
        pool.release(second).await().atMost(TIMEOUT);
    }

    @Test
    void dirtyConnectionIsPutBackOnTheDefaultDatabaseOnRelease() {
        // A URI naming a database makes every fresh connection start there, so that, not 0, is the
        // state to restore.
        RedisURI uri = RedisURI.Builder.redis(REDIS.getHost(), REDIS.getFirstMappedPort()).withDatabase(2).build();
        LettuceConnectionPool pool = new LettuceConnectionPool(
                () -> redisClient.connectAsync(codec, uri).thenApply(LettuceConnection::standalone),
                1, 1, 2);
        try {
            LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
            assertThat(sync(conn).clientInfo()).contains(" db=2 ");
            pool.markDirty(conn);
            sync(conn).select(1);
            pool.release(conn).await().atMost(TIMEOUT);
            assertThat(pool.isDirty(conn)).isFalse();

            LettuceConnection again = pool.acquireBlocking(TIMEOUT);
            assertThat(again).isSameAs(conn);
            assertThat(sync(again).clientInfo()).contains(" db=2 ");
            pool.release(again).await().atMost(TIMEOUT);
        } finally {
            pool.close().await().atMost(TIMEOUT);
        }
    }

    @Test
    void releaseOnlyIssuesSelectForDirtyConnections() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        long id = sync(conn).clientId();
        pool.release(conn).await().atMost(TIMEOUT);
        assertThat(lastCommandOf(id)).isEqualTo("client|id");

        conn = pool.acquireBlocking(TIMEOUT);
        sync(conn).clientId();
        pool.markDirty(conn);
        pool.release(conn).await().atMost(TIMEOUT);
        assertThat(lastCommandOf(id)).isEqualTo("select");
    }

    @Test
    void dirtyConnectionWhoseResetFailsIsNotHandedOutAndStaysMarked() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        pool.markDirty(conn);
        conn.close(); // the reset SELECT cannot succeed on a closed connection
        pool.release(conn).await().atMost(TIMEOUT);
        assertThat(pool.isDirty(conn)).isTrue();

        // The retry on acquire fails too: the caller gets the failure instead of a connection on an
        // unknown database, and the connection goes back to the pool still marked.
        assertThatThrownBy(() -> pool.acquireBlocking(TIMEOUT)).isInstanceOf(RedisException.class);
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
        assertThat(pool.isDirty(conn)).isTrue();
    }

    @Test
    void cancellationDequeuesWaiterFreeingItsWaitingSlot() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection held = pool.acquireBlocking(TIMEOUT);

        Cancellable firstWaiter = pool.withPooled(conn -> Uni.createFrom().voidItem()).subscribe().with(c -> {
        }, t -> {
        });
        firstWaiter.cancel();

        // The canceled waiter must have freed its slot: this one queues instead of being rejected,
        // proving the waiting counter was decremented on cancellation rather than leaked.
        AtomicBoolean secondWaiterCompleted = new AtomicBoolean();
        AtomicReference<Throwable> secondWaiterFailure = new AtomicReference<>();
        pool.withPooled(conn -> Uni.createFrom().voidItem())
                .subscribe().with(c -> secondWaiterCompleted.set(true), secondWaiterFailure::set);
        assertThat(secondWaiterFailure.get()).isNull();
        assertThat(secondWaiterCompleted).isFalse();

        pool.release(held).await().atMost(TIMEOUT);
        await().atMost(TIMEOUT).until(secondWaiterCompleted::get);
        assertThat(secondWaiterFailure.get()).isNull();
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
    }

    @Test
    void closeCausesFurtherAcquireToFail() {
        LettuceConnectionPool pool = pool(1, 1);
        pool.close().await().atMost(TIMEOUT);
        assertThatThrownBy(() -> pool.acquireBlocking(TIMEOUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void closeFailsQueuedWaitersInsteadOfLeavingThemHanging() throws Exception {
        LettuceConnectionPool pool = pool(1, 1);
        pool.acquireBlocking(TIMEOUT); // hold the only connection so the next caller queues

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                pool.acquireBlocking(TIMEOUT);
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        waiter.start();
        await().atMost(TIMEOUT).until(() -> waiter.getState() == Thread.State.TIMED_WAITING);

        pool.close().await().atMost(TIMEOUT);

        // Well within the waiter's own timeout: it was failed by close(), not by giving up.
        waiter.join(Duration.ofSeconds(1).toMillis());
        assertThat(waiter.isAlive()).isFalse();
        assertThat(failure.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void cancellingWithPooledDoesNotFreeConnectionUntilTheRealCommandCompletes() {
        LettuceConnectionPool pool = pool(1, 1);
        String missingKey = UUID.randomUUID().toString();

        Cancellable subscription = pool
                .withPooled(conn -> LettuceResult
                        .toUni(() -> conn.async().blpop(30L, missingKey.getBytes(StandardCharsets.UTF_8))))
                .subscribe().with(item -> {
                }, failure -> {
                });
        // Wait for the connection to actually be borrowed (and the BLPOP dispatched) before
        // cancelling, so this exercises "cancel while a real command is in flight" rather than
        // racing the still-in-progress connect.
        await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 1);
        subscription.cancel();

        // The caller gave up, but the BLPOP is still pending on the wire: the connection must stay
        // checked out, not be returned to the pool as idle.
        assertThat(pool.getIdle()).isZero();

        // Only the real Redis reply frees the connection.
        rawPush(missingKey, "unblocked");
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
    }

    @Test
    void cancellingWithPooledWhileThePoolIsStillConnectingReturnsTheConnection() {
        // Gate the connector so the cancel provably lands while the underlying connect is in flight.
        CompletableFuture<Void> gate = new CompletableFuture<>();
        LettuceConnectionPool pool = new LettuceConnectionPool(() -> gate.thenCompose(v -> connectAsync()), 1, 1, 0);

        AtomicBoolean bodyRan = new AtomicBoolean();
        Cancellable subscription = pool.withPooled(conn -> {
            bodyRan.set(true);
            return Uni.createFrom().voidItem();
        }).subscribe().with(x -> {
        }, t -> {
        });
        subscription.cancel();
        gate.complete(null);

        // The connection the pool finished creating for the cancelled caller must end up idle in
        // the pool, not handed to a dead request (a silent no-op) and thereby leaked.
        await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 1);
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
        assertThat(bodyRan).isFalse();

        // With maxTotal=1 this would queue forever had the connection leaked.
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        pool.release(conn).await().atMost(TIMEOUT);
    }

    @Test
    void waiterCancellingConcurrentlyWithReleaseNeverLeaksTheConnection() throws Exception {
        // release() dequeues a waiter and then completes it. A cancel landing in that window used
        // to make the hand-over a silent no-op, leaking the connection. There is no hook to inject
        // into that window, so race the two from separate threads many times: the connection must
        // always end up idle again, whichever side wins. (Handing over through a Uni item cannot
        // pass this test: Mutiny drops an item that arrives after a cancel without telling the
        // emitter — hence every request hands over through a callback that reports whether it
        // was still live.)
        LettuceConnectionPool pool = pool(1, 1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 300; i++) {
                LettuceConnection held = pool.acquireBlocking(TIMEOUT);
                Cancellable waiter = pool.withPooled(conn -> Uni.createFrom().voidItem()).subscribe().with(x -> {
                }, t -> {
                });

                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> cancel = executor.submit(() -> {
                    barrier.await();
                    waiter.cancel();
                    return null;
                });
                Future<?> release = executor.submit(() -> {
                    barrier.await();
                    pool.release(held).await().atMost(TIMEOUT);
                    return null;
                });
                cancel.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                release.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

                await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(1)).until(() -> pool.getIdle() == 1);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(pool.getObjectCount()).isEqualTo(1);
    }

    @Test
    void releaseRacingAnExhaustedAcquireNeverLosesTheWakeUp() throws Exception {
        // Lost wake-up: an acquire finds the pool exhausted, and before it is queued a concurrent
        // release() finds no waiter and returns the connection to Lettuce's idle cache. The queued
        // caller would then wait for a release that never comes. The pool lock makes the two
        // decisions mutually exclusive; there is no hook into the window, so race them many times.
        LettuceConnectionPool pool = pool(1, 1);
        // Mutiny's context-propagation hook is registered lazily on first use and is not safe to
        // race from two threads; run through it once here so the race below only races the pool.
        pool.release(pool.acquireBlocking(TIMEOUT)).await().atMost(TIMEOUT);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 300; i++) {
                LettuceConnection held = pool.acquireBlocking(TIMEOUT);

                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> release = executor.submit(() -> {
                    barrier.await();
                    pool.release(held).await().atMost(TIMEOUT);
                    return null;
                });
                Future<Long> acquire = executor.submit(() -> {
                    barrier.await();
                    return pool.withPooled(conn -> LettuceResult.toUni(() -> conn.async().clientId()))
                            .await().atMost(TIMEOUT);
                });
                release.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                assertThat(acquire.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isNotNull();

                await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(1)).until(() -> pool.getIdle() == 1);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(pool.getObjectCount()).isEqualTo(1);
    }

    @Test
    void acquireBlockingTimingOutWhileThePoolIsStillConnectingReturnsTheConnection() {
        CompletableFuture<Void> gate = new CompletableFuture<>();
        LettuceConnectionPool pool = new LettuceConnectionPool(() -> gate.thenCompose(v -> connectAsync()), 1, 1, 0);

        assertThatThrownBy(() -> pool.acquireBlocking(Duration.ofMillis(200)))
                .isInstanceOf(io.smallrye.mutiny.TimeoutException.class);
        gate.complete(null);

        // The connection that finished connecting after the caller gave up goes back to the pool.
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);

        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        assertThat(sync(conn).ping()).isEqualTo("PONG");
        pool.release(conn).await().atMost(TIMEOUT);
    }

    @Test
    void acquireBlockingQueuesWhenExhaustedAndFailsWhenTooManyWait() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection held = pool.acquireBlocking(TIMEOUT);

        // First extra caller queues (maxWaiting=1) and times out; the slot it held must be freed...
        assertThatThrownBy(() -> pool.acquireBlocking(Duration.ofMillis(200)))
                .isInstanceOf(io.smallrye.mutiny.TimeoutException.class);
        // ...so this one queues too, instead of being rejected as "too many waiting".
        AtomicReference<LettuceConnection> handedOver = new AtomicReference<>();
        Thread waiter = new Thread(() -> handedOver.set(pool.acquireBlocking(TIMEOUT)));
        waiter.start();
        await().atMost(TIMEOUT).until(() -> waiter.getState() == Thread.State.TIMED_WAITING);

        // With the queue full (maxWaiting=1), a further caller is rejected immediately.
        assertThatThrownBy(() -> pool.acquireBlocking(TIMEOUT))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("too many waiting requests");

        pool.release(held).await().atMost(TIMEOUT);
        await().atMost(TIMEOUT).until(() -> handedOver.get() != null);
        assertThat(handedOver.get()).isSameAs(held);
        pool.release(handedOver.get()).await().atMost(TIMEOUT);
    }

    @Test
    void cancellingWithScopedCancelsTheBodyAndReleasesTheConnection() {
        LettuceConnectionPool pool = pool(1, 1);
        AtomicBoolean bodyCancelled = new AtomicBoolean();

        Cancellable subscription = pool
                .withScoped(conn -> Uni.createFrom().<Void> emitter(e -> {
                    // never completes
                }).onCancellation().invoke(() -> bodyCancelled.set(true)))
                .subscribe().with(x -> {
                }, t -> {
                });
        await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 1 && pool.getIdle() == 0);

        subscription.cancel();

        // Unlike withPooled, a scoped connection follows the caller: cancelling propagates to the
        // body and frees the connection right away.
        assertThat(bodyCancelled).isTrue();
        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
    }

    @Test
    void cancellingWithScopedBeforeHandOverReturnsTheConnection() {
        CompletableFuture<Void> gate = new CompletableFuture<>();
        LettuceConnectionPool pool = new LettuceConnectionPool(() -> gate.thenCompose(v -> connectAsync()), 1, 1, 0);
        AtomicBoolean bodyRan = new AtomicBoolean();

        Cancellable subscription = pool.withScoped(conn -> {
            bodyRan.set(true);
            return Uni.createFrom().voidItem();
        }).subscribe().with(x -> {
        }, t -> {
        });
        subscription.cancel();
        gate.complete(null);

        await().atMost(TIMEOUT).until(() -> pool.getIdle() == 1);
        assertThat(bodyRan).isFalse();
    }

    @Test
    void withPooledReleasesConnectionWhenBodyThrowsSynchronously() {
        LettuceConnectionPool pool = pool(1, 1);
        IllegalArgumentException boom = new IllegalArgumentException("bad args");

        // The body throws from apply() itself (like eager argument validation), before ever
        // returning a Uni to subscribe to. The failure must reach the caller...
        assertThatThrownBy(() -> pool.<Void> withPooled(conn -> {
            throw boom;
        }).await().atMost(TIMEOUT)).isSameAs(boom);

        // ...and the connection must be back in the pool, not leaked as checked-out forever.
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getIdle()).isEqualTo(1);

        // With maxTotal=1, this would queue forever (and time out) had the connection leaked.
        Long pong = pool.withPooled(conn -> LettuceResult.toUni(() -> conn.async().clientId()))
                .await().atMost(TIMEOUT);
        assertThat(pong).isNotNull();
        assertThat(pool.getIdle()).isEqualTo(1);
    }

    @Test
    void connectFailureWakesQueuedWaitersInsteadOfLeavingThemHanging() {
        // Lettuce counts an in-flight connect towards maxTotal, so with maxTotal=1 a second caller
        // queues behind the first caller's connect. If that connect fails, nothing is ever released,
        // so unless the queue is re-driven on failure the second caller hangs forever.
        CompletableFuture<Void> gate = new CompletableFuture<>();
        AtomicReference<Supplier<CompletionStage<LettuceConnection>>> connector = new AtomicReference<>(
                () -> gate.thenCompose(v -> connectAsync()));
        LettuceConnectionPool pool = new LettuceConnectionPool(() -> connector.get().get(), 1, 1, 0);

        AtomicBoolean anyBodyRan = new AtomicBoolean();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        pool.withPooled(conn -> {
            anyBodyRan.set(true);
            return Uni.createFrom().voidItem();
        }).subscribe().with(x -> {
        }, firstFailure::set);
        pool.withPooled(conn -> {
            anyBodyRan.set(true);
            return Uni.createFrom().voidItem();
        }).subscribe().with(x -> {
        }, secondFailure::set);
        // Both are pending: the first on its connect, the second queued behind it.
        assertThat(firstFailure.get()).isNull();
        assertThat(secondFailure.get()).isNull();

        ConnectException boom = new ConnectException("Redis is unreachable");
        gate.completeExceptionally(boom);

        // The second caller retries its acquire, which starts a fresh connect that fails the same
        // way: it gets the failure rather than staying parked in the queue.
        await().atMost(TIMEOUT).until(() -> firstFailure.get() != null && secondFailure.get() != null);
        assertThat(firstFailure.get()).hasRootCause(boom);
        assertThat(secondFailure.get()).hasRootCause(boom);
        assertThat(anyBodyRan).isFalse();
        assertThat(pool.getObjectCount()).isZero();

        // Redis comes back: the pool is unharmed and hands out connections again.
        connector.set(CommandsTestBase::connectAsync);
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        assertThat(sync(conn).ping()).isEqualTo("PONG");
        pool.release(conn).await().atMost(TIMEOUT);
        assertThat(pool.getIdle()).isEqualTo(1);
    }

    @Test
    void idleConnectionIsRecycledAfterTheRecycleTimeout() {
        LettuceConnectionPool pool = pool(2, 1, Duration.ofMillis(300), Duration.ofMillis(50));
        try {
            LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
            long id = sync(conn).clientId();
            pool.release(conn).await().atMost(TIMEOUT);
            assertThat(pool.getIdle()).isEqualTo(1);

            // the cleaner closes it once it has been idle for the recycle timeout...
            await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 0);
            assertThat(pool.getIdle()).isZero();
            await().atMost(TIMEOUT).until(() -> !connection.sync().clientList().contains("id=" + id + " "));
            assertThat(conn.isOpen()).isFalse();

            // ...and the next borrower gets a fresh one
            LettuceConnection fresh = pool.acquireBlocking(TIMEOUT);
            assertThat(sync(fresh).clientId()).isNotEqualTo(id);
            pool.release(fresh).await().atMost(TIMEOUT);
        } finally {
            pool.close().await().atMost(TIMEOUT);
        }
    }

    @Test
    void checkedOutConnectionIsNotRecycled() throws Exception {
        LettuceConnectionPool pool = pool(1, 1, Duration.ofMillis(200), Duration.ofMillis(50));
        try {
            LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
            // several cleaner runs past the recycle timeout while the connection is checked out
            Thread.sleep(600);
            assertThat(conn.isOpen()).isTrue();
            assertThat(sync(conn).ping()).isEqualTo("PONG");
            assertThat(pool.getObjectCount()).isEqualTo(1);
            pool.release(conn).await().atMost(TIMEOUT);
            // released, it is recycled like any idle connection
            await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 0);
        } finally {
            pool.close().await().atMost(TIMEOUT);
        }
    }

    @Test
    void mostRecentlyReleasedConnectionIsReusedSoTheOthersAgeOut() throws Exception {
        LettuceConnectionPool pool = pool(2, 1, Duration.ofMillis(400), Duration.ofMillis(50));
        try {
            LettuceConnection first = pool.acquireBlocking(TIMEOUT);
            LettuceConnection second = pool.acquireBlocking(TIMEOUT);
            pool.release(first).await().atMost(TIMEOUT);
            pool.release(second).await().atMost(TIMEOUT);

            // a borrower every 60 ms gets the most recently released connection back each time, so the other
            // one stays idle from its release on
            for (int i = 0; i < 10; i++) {
                LettuceConnection borrowed = pool.acquireBlocking(TIMEOUT);
                assertThat(borrowed).isSameAs(second);
                pool.release(borrowed).await().atMost(TIMEOUT);
                Thread.sleep(60);
            }

            // well past the recycle timeout: the idle one is gone, the busy one is kept
            await().atMost(TIMEOUT).until(() -> pool.getObjectCount() == 1);
            assertThat(first.isOpen()).isFalse();
            assertThat(second.isOpen()).isTrue();
            assertThat(pool.getIdle()).isEqualTo(1);
        } finally {
            pool.close().await().atMost(TIMEOUT);
        }
    }

    @Test
    void recyclingIsOffWithoutAPositiveCleanerInterval() throws Exception {
        LettuceConnectionPool pool = pool(1, 1, Duration.ofMillis(100), Duration.ZERO);
        try {
            LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
            pool.release(conn).await().atMost(TIMEOUT);
            Thread.sleep(300);
            assertThat(pool.getIdle()).isEqualTo(1);
            assertThat(conn.isOpen()).isTrue();
        } finally {
            pool.close().await().atMost(TIMEOUT);
        }
    }

    @Test
    void closeClosesTheIdleConnections() {
        LettuceConnectionPool pool = pool(2, 1);
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        pool.release(conn).await().atMost(TIMEOUT);

        pool.close().await().atMost(TIMEOUT);

        assertThat(conn.isOpen()).isFalse();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getIdle()).isZero();
    }

    @Test
    void connectionReleasedAfterCloseIsClosed() {
        LettuceConnectionPool pool = pool(1, 1);
        LettuceConnection conn = pool.acquireBlocking(TIMEOUT);
        pool.close().await().atMost(TIMEOUT);

        pool.release(conn).await().atMost(TIMEOUT);

        assertThat(conn.isOpen()).isFalse();
        assertThat(pool.getObjectCount()).isZero();
    }

    private static void rawPush(String key, String value) {
        connection.sync().rpush(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }

}

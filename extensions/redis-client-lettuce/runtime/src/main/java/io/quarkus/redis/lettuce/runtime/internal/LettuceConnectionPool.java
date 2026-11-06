package io.quarkus.redis.lettuce.runtime.internal;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Bounded pool of {@link LettuceConnection}s (to a standalone server or to a cluster, see
 * {@link LettuceConnectionFactory}), used for blocking commands and scoped connections
 * ({@code withConnection}/{@code withTransaction}) so that they never occupy the shared multiplexed
 * connection used by ordinary commands.
 * <p>
 * At most {@code maxPoolSize} connections exist at a time, those being opened included; once they are all
 * checked out, further requests queue, up to {@code maxWaiting} of them, mirroring the Vert.x backend's
 * {@code max-pool-size} and {@code max-pool-waiting}. Idle connections are handed out most recently
 * released first, so that the ones a lighter load no longer needs age out: a cleaner, running every
 * {@code cleanerInterval}, closes the connections idle for longer than {@code recycleTimeout}, as the
 * Vert.x client does with {@code pool-cleaner-interval} and {@code pool-recycle-timeout}. A connection
 * that is checked out is never recycled.
 * <p>
 * Pooled connections must come back in the state a fresh connection has, or the next borrower
 * inherits it. {@code MULTI}/{@code WATCH} are undone by the transaction paths; {@code SELECT} is
 * undone here: a borrower that changes the database calls {@link #markDirty(LettuceConnection)},
 * and {@link #release(LettuceConnection)} issues {@code SELECT <defaultDatabase>} before the
 * connection is handed on. If that reset fails, the connection stays marked and the reset is retried
 * before it is next handed out; a borrower is never given a connection on the wrong database.
 * A cluster has a single database, so cluster connections are never marked: a {@code SELECT} is
 * rejected by the server and leaves nothing to reset.
 */
public final class LettuceConnectionPool {

    private static final Logger LOGGER = Logger.getLogger(LettuceConnectionPool.class);

    private final Supplier<CompletionStage<LettuceConnection>> connector;
    private final int maxPoolSize;
    private final int maxWaiting;
    /** Database a fresh connection is on ({@code RedisURI.getDatabase()}), restored on release. */
    private final int defaultDatabase;
    /** Connections whose selected database may differ from {@link #defaultDatabase}. */
    private final Set<LettuceConnection> dirty = ConcurrentHashMap.newKeySet();

    /**
     * Serializes "connection available, else queue me" against "anyone queued, else keep the connection
     * idle", so a release cannot slip between an acquire seeing the pool exhausted and that acquire being
     * queued (a lost wake-up), and guards the idle connections and the connection count. Nothing that can
     * run user code or I/O — completing a request, subscribing a body, opening or closing a connection —
     * happens while it is held.
     */
    private final Object lock = new Object();
    /** Guarded by {@link #lock}. The idle connections, the most recently released first. */
    private final Deque<Idle> idle = new ArrayDeque<>();
    /** Guarded by {@link #lock}. The connections that exist or are being opened: idle, checked out or connecting. */
    private int total;
    /** Guarded by {@link #lock}. Every queued request is live: not yet completed, failed or abandoned. */
    private final Deque<Request> waiters = new ArrayDeque<>();
    /** Guarded by {@link #lock}. */
    private boolean closed;

    /** Schedules the cleaner; {@code null} when the pool does not recycle its connections. */
    private final Vertx vertx;
    private final long recycleTimeoutNanos;
    private final long cleanerTimerId;

    /**
     * A pool that never recycles its idle connections.
     *
     * @param connector opens a new connection; the connection must start on {@code defaultDatabase}
     * @param maxPoolSize maximum number of connections
     * @param maxWaiting maximum number of requests queued once {@code maxPoolSize} are checked out
     * @param defaultDatabase database of a fresh connection, restored before a connection is reused
     *        after a borrower changed it
     */
    public LettuceConnectionPool(Supplier<CompletionStage<LettuceConnection>> connector,
            int maxPoolSize, int maxWaiting, int defaultDatabase) {
        this(connector, maxPoolSize, maxWaiting, defaultDatabase, null, null, null);
    }

    /**
     * @param connector opens a new connection; the connection must start on {@code defaultDatabase}
     * @param maxPoolSize maximum number of connections
     * @param maxWaiting maximum number of requests queued once {@code maxPoolSize} are checked out
     * @param defaultDatabase database of a fresh connection, restored before a connection is reused
     *        after a borrower changed it
     * @param vertx schedules the cleaner; {@code null} for a pool that never recycles its connections
     * @param recycleTimeout how long a connection may stay idle before the cleaner closes it; recycling is
     *        off when {@code null} or not positive
     * @param cleanerInterval how often the cleaner runs; recycling is off when {@code null} or not positive
     */
    public LettuceConnectionPool(Supplier<CompletionStage<LettuceConnection>> connector,
            int maxPoolSize, int maxWaiting, int defaultDatabase,
            Vertx vertx, Duration recycleTimeout, Duration cleanerInterval) {
        this.connector = connector;
        this.maxPoolSize = maxPoolSize;
        this.maxWaiting = maxWaiting;
        this.defaultDatabase = defaultDatabase;
        boolean recycling = vertx != null && isPositive(recycleTimeout) && isPositive(cleanerInterval);
        this.vertx = recycling ? vertx : null;
        this.recycleTimeoutNanos = recycling ? recycleTimeout.toNanos() : 0;
        this.cleanerTimerId = recycling ? vertx.setPeriodic(Math.max(1, cleanerInterval.toMillis()), id -> recycle()) : -1;
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    /**
     * Records that {@code connection} may no longer be on {@link #defaultDatabase}, so that it is
     * reset before anyone else uses it. Called by a borrower before it issues {@code SELECT}. A
     * cluster connection is never marked: a cluster has a single database, so the {@code SELECT}
     * the borrower is about to issue is rejected and leaves nothing to reset.
     */
    public void markDirty(LettuceConnection connection) {
        if (!connection.isCluster()) {
            dirty.add(connection);
        }
    }

    /**
     * Whether {@code connection} is marked as possibly being on the wrong database.
     */
    public boolean isDirty(LettuceConnection connection) {
        return dirty.contains(connection);
    }

    /**
     * Blocks until a connection is available or {@code timeout} elapses. On timeout (or interrupt)
     * the request is withdrawn from the queue and a connection that arrives just as the caller
     * gives up is returned to the pool rather than leaked.
     */
    public LettuceConnection acquireBlocking(Duration timeout) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException("acquireBlocking must not be called from an event loop thread");
        }
        Request request = new Request();
        request.start();
        try {
            return request.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            request.abandon();
            request.thenAccept(this::returnToPool);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
            throw new io.smallrye.mutiny.TimeoutException();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new CompletionException(e.getCause());
        }
    }

    /**
     * Releases a connection back to the pool, handing it straight to the next waiter if any. A
     * connection marked by {@link #markDirty(LettuceConnection)} is first put back on
     * {@link #defaultDatabase}; if that fails it is handed on still marked, and the reset is
     * retried before the next borrower gets it. A connection released after {@link #close()} is closed.
     */
    public Uni<Void> release(LettuceConnection connection) {
        if (!dirty.contains(connection)) {
            return handOver(connection);
        }
        return resetDatabase(connection)
                .onFailure().invoke(failure -> LOGGER.warnf(failure,
                        "Failed to reset pooled Redis connection to database %d on release, will retry before its next use",
                        defaultDatabase))
                .onFailure().recoverWithNull()
                .chain(() -> handOver(connection));
    }

    private Uni<Void> handOver(LettuceConnection connection) {
        while (true) {
            Request waiter;
            synchronized (lock) {
                waiter = waiters.poll();
                if (waiter == null) {
                    if (!closed) {
                        idle.addFirst(new Idle(connection, System.nanoTime()));
                        return Uni.createFrom().voidItem();
                    }
                    total--;
                }
            }
            if (waiter == null) {
                // the pool was closed while the connection was checked out
                return Uni.createFrom().completionStage(destroy(connection));
            }
            if (waiter.deliver(connection)) {
                return Uni.createFrom().voidItem();
            }
            // Abandoned between being polled and being completed: pick another.
        }
    }

    /**
     * Issues {@code SELECT defaultDatabase} and clears the dirty mark once Redis confirms it.
     * Lettuce records the confirmed database in its connection state, so a later reconnect
     * replays the default database rather than the one the borrower selected.
     */
    private Uni<Void> resetDatabase(LettuceConnection connection) {
        return LettuceResult.toUni(() -> connection.select(defaultDatabase))
                .invoke(() -> dirty.remove(connection))
                .replaceWithVoid();
    }

    private void redrive() {
        Request next;
        synchronized (lock) {
            next = waiters.poll();
        }
        if (next != null) {
            next.start();
        }
    }

    private Uni<Void> releaseQuietly(LettuceConnection connection) {
        return release(connection)
                .onFailure().invoke(failure -> LOGGER.warnf(failure,
                        "Failed to release pooled Redis connection back to the pool"))
                .onFailure().recoverWithNull();
    }

    private void returnToPool(LettuceConnection connection) {
        releaseQuietly(connection).subscribe().with(ignored -> {
        });
    }

    /**
     * Opens a connection for a request that found none idle. Never called while holding the lock.
     */
    private CompletableFuture<LettuceConnection> open() {
        try {
            return connector.get().toCompletableFuture();
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Closes a connection that left the pool. Never called while holding the lock.
     */
    private CompletableFuture<Void> destroy(LettuceConnection connection) {
        dirty.remove(connection);
        return connection.closeAsync().exceptionally(failure -> {
            LOGGER.debugf(failure, "Failed to close a pooled Redis connection");
            return null;
        });
    }

    /**
     * The cleaner: closes the connections idle for longer than the recycle timeout. The idle connections are
     * ordered by their release, so the stale ones are at the tail.
     */
    private void recycle() {
        long now = System.nanoTime();
        List<LettuceConnection> stale = new ArrayList<>();
        synchronized (lock) {
            if (closed) {
                return;
            }
            Idle oldest;
            while ((oldest = idle.peekLast()) != null && now - oldest.since() >= recycleTimeoutNanos) {
                idle.pollLast();
                total--;
                stale.add(oldest.connection());
            }
        }
        if (!stale.isEmpty()) {
            LOGGER.debugf("Closing %d pooled Redis connection(s) idle for longer than %d ms", stale.size(),
                    TimeUnit.NANOSECONDS.toMillis(recycleTimeoutNanos));
            for (LettuceConnection connection : stale) {
                destroy(connection);
            }
        }
    }

    /**
     * Runs {@code body} on a pooled connection, releasing it only once {@code body} truly
     * completes (item or failure) — never merely because the caller stopped waiting.
     */
    public <T> Uni<T> withPooled(Function<LettuceConnection, Uni<T>> body) {
        return run(body, false);
    }

    /**
     * Runs {@code body} on a pooled connection that is scoped to the caller's subscription: the
     * caller's cancellation is propagated to {@code body}, and the connection is released on any
     * termination of {@code body} — item, failure or cancellation. This is the shape used by
     * {@code withConnection} and {@code withTransaction}.
     */
    public <T> Uni<T> withScoped(Function<LettuceConnection, Uni<T>> body) {
        return run(body, true);
    }

    private <T> Uni<T> run(Function<LettuceConnection, Uni<T>> body, boolean scoped) {
        return Uni.createFrom().emitter(emitter -> {
            Request request = new Request();
            AtomicReference<Cancellable> running = new AtomicReference<>();
            request.whenComplete((conn, failure) -> {
                if (failure != null) {
                    emitter.fail(failure);
                    return;
                }
                Cancellable subscription = Uni.createFrom().deferred(() -> body.apply(conn))
                        .onTermination().call(() -> releaseQuietly(conn))
                        .subscribe().with(emitter::complete, emitter::fail);
                if (scoped) {
                    running.set(subscription);
                }
            });
            emitter.onTermination(() -> {
                request.abandon();
                Cancellable subscription = running.get();
                if (subscription != null) {
                    subscription.cancel();
                }
            });
            request.start();
        });
    }

    /**
     * Closes the pool: the cleaner stops and the idle connections are closed; the checked-out ones are
     * closed as they are released. Requests still queued for a connection fail with an
     * {@link IllegalStateException}, as do any made afterward.
     */
    public Uni<Void> close() {
        List<Request> queued;
        List<Idle> released;
        synchronized (lock) {
            closed = true;
            queued = new ArrayList<>(waiters);
            waiters.clear();
            released = new ArrayList<>(idle);
            idle.clear();
            total -= released.size();
        }
        if (vertx != null) {
            vertx.cancelTimer(cleanerTimerId);
        }
        for (Request waiter : queued) {
            waiter.completeExceptionally(new IllegalStateException("Redis connection pool is closed"));
        }
        List<CompletableFuture<Void>> closing = new ArrayList<>(released.size());
        for (Idle entry : released) {
            closing.add(destroy(entry.connection()));
        }
        return Uni.createFrom().completionStage(CompletableFuture.allOf(closing.toArray(CompletableFuture[]::new)));
    }

    /**
     * The connections that exist or are being opened: idle, checked out or connecting.
     */
    public int getObjectCount() {
        synchronized (lock) {
            return total;
        }
    }

    public int getIdle() {
        synchronized (lock) {
            return idle.size();
        }
    }

    /** An idle connection and when it became idle ({@link System#nanoTime()}). */
    private record Idle(LettuceConnection connection, long since) {
    }

    private final class Request extends CompletableFuture<LettuceConnection> {

        void start() {
            LettuceConnection available = null;
            boolean opening = false;
            RuntimeException rejection = null;
            synchronized (lock) {
                if (isDone()) {
                    return; // abandoned before we got here
                }
                if (closed) {
                    rejection = new IllegalStateException("Redis connection pool is closed");
                } else {
                    Idle entry = idle.pollFirst();
                    if (entry != null) {
                        available = entry.connection();
                    } else if (total < maxPoolSize) {
                        total++;
                        opening = true;
                    } else if (waiters.size() < maxWaiting) {
                        waiters.add(this);
                        return;
                    } else {
                        rejection = new NoSuchElementException("Redis connection pool exhausted, too many waiting requests");
                    }
                }
            }
            if (rejection != null) {
                completeExceptionally(rejection);
                return;
            }
            if (!opening) {
                if (!deliver(available)) {
                    returnToPool(available);
                }
                return;
            }
            open().whenComplete((conn, failure) -> {
                if (failure == null) {
                    if (!deliver(conn)) {
                        returnToPool(conn);
                    }
                } else {
                    synchronized (lock) {
                        total--;
                    }
                    completeExceptionally(failure);
                    redrive();
                }
            });
        }

        /**
         * Hands {@code conn} to this request. Returns {@code false} if the request was already
         * abandoned and {@code conn} is still the caller's to return. A connection still marked
         * dirty (its reset failed on release) is reset first; if that fails again the request
         * fails and the connection goes back to the pool, still marked, rather than being handed
         * out on the wrong database.
         */
        boolean deliver(LettuceConnection conn) {
            if (!dirty.contains(conn)) {
                return complete(conn);
            }
            if (isDone()) {
                return false;
            }
            resetDatabase(conn).subscribe().with(
                    ignored -> {
                        if (!complete(conn)) {
                            returnToPool(conn);
                        }
                    },
                    failure -> {
                        completeExceptionally(failure);
                        returnToPool(conn);
                    });
            return true;
        }

        void abandon() {
            synchronized (lock) {
                waiters.remove(this);
            }
            cancel(false);
        }

    }

}

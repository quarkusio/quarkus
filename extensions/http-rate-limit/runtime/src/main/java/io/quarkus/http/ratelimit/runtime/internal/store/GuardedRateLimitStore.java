package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.http.ConnectionPoolTooBusyException;

/**
 * Wraps a non-local store in its fault tolerance: a per-call deadline, a concurrency limit, the shared circuit breaker
 * and timeout of the {@link Guard}, and {@code store-failure-mode} when any of them refuses or the store fails.
 * <p>
 * The guard is built once per store by {@link StoreRegistry} and shared; this class only wraps calls and
 * implements the failure modes.
 */
final class GuardedRateLimitStore implements RateLimitStore {

    private static final Logger LOG = Logger.getLogger(GuardedRateLimitStore.class);

    private static final TypeLiteral<Uni<RateLimitDecision>> DECISION = new TypeLiteral<>() {
    };

    /**
     * Thrown by the Vert.x clients, the Redis client included, when their connection pool and its wait queue are full,
     * which says nothing about the health of the store: the call fails, and {@code store-failure-mode} decides its
     * request, but the circuit breaker skips it (SmallRye Fault Tolerance counts a skipped exception as a success).
     */
    static final Class<ConnectionPoolTooBusyException> POOL_REFUSAL = ConnectionPoolTooBusyException.class;

    /**
     * Which authority produced a grant.
     */
    enum Source {
        STORE,
        LOCAL_FALLBACK,
        OPEN_FALLBACK,
        CLOSED_FALLBACK
    }

    record Granted(RateLimitDecision decision, Source source, Duration retryAfter) {
        Granted(RateLimitDecision decision, Source source) {
            this(decision, source, null);
        }

        static Granted unavailable(Duration retryAfter) {
            return new Granted(null, Source.CLOSED_FALLBACK, retryAfter);
        }

        RateLimitDecision decisionOrThrow() {
            if (source == Source.CLOSED_FALLBACK) {
                throw new StoreUnavailableException(retryAfter);
            }
            return decision;
        }
    }

    private final String name;
    private final Guard guard;
    private final AtomicReference<CircuitBreakerState> breakerState;
    private final Duration breakerDelay;
    private final AtomicInteger inFlight;
    private final int maxConcurrentRequests;
    private final RateLimitStore delegate;
    private final Duration timeout;
    private final FailureMode failureMode;
    private final LocalRateLimitStore local;
    private final RateLimitClock clock;

    /**
     * @param breakerState kept current by the guard's {@code onStateChange}, including the timer-driven OPEN to
     *        HALF_OPEN transition inside SmallRye
     * @param breakerDelay the delay of the breaker, the {@code Retry-After} under {@code store-failure-mode=closed}
     * @param name the store name
     * @param inFlight the calls in flight on this store, shared like the guard
     * @param maxConcurrentRequests the most calls in flight on this store at once
     * @param timeout the time budget of one call to this store
     * @param failureMode the failure mode of this store
     */
    GuardedRateLimitStore(String name, Guard guard, AtomicReference<CircuitBreakerState> breakerState,
            Duration breakerDelay, AtomicInteger inFlight, int maxConcurrentRequests, RateLimitStore delegate,
            Duration timeout, FailureMode failureMode,
            LocalRateLimitStore local, RateLimitClock clock) {
        this.name = name;
        this.guard = guard;
        this.breakerState = breakerState;
        this.breakerDelay = breakerDelay;
        this.inFlight = inFlight;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.delegate = delegate;
        this.timeout = timeout;
        this.failureMode = failureMode;
        this.local = local;
        this.clock = clock;
    }

    /**
     * @return whether the breaker is OPEN or HALF_OPEN
     */
    boolean degraded() {
        return breakerState.get() != CircuitBreakerState.CLOSED;
    }

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        return grant(key, cost, policy).map(new Function<>() {
            @Override
            public RateLimitDecision apply(Granted granted) {
                return granted.decisionOrThrow();
            }
        });
    }

    /**
     * {@link #tryConsume}, telling whether the store or the failure mode decided.
     */
    Uni<Granted> grant(String key, long cost, RateLimitPolicy policy) {
        return attempt(key, cost, policy)
                .map(new Function<RateLimitDecision, Granted>() {
                    @Override
                    public Granted apply(RateLimitDecision decision) {
                        return new Granted(decision, Source.STORE);
                    }
                })
                .onFailure().recoverWithUni(new Function<>() {
                    @Override
                    public Uni<? extends Granted> apply(Throwable failure) {
                        LOG.debugf(failure, "Rate limit store '%s' call failed; store-failure-mode=%s applies", name,
                                failureMode.name().toLowerCase(Locale.ROOT));
                        return fallback(key, cost, policy);
                    }
                });
    }

    /**
     * A zero-cost call that writes nothing, through the same path as every other call; it has no fallback, since its
     * only purpose is to give the breaker a real outcome.
     */
    Uni<RateLimitDecision> probe(RateLimitPolicy policy) {
        return attempt("probe", 0, policy);
    }

    /**
     * The guarded store call alone: a failure (timeout, concurrency limit, open breaker, store error) is propagated,
     * for decorators that choose the cost of the {@linkplain #fallback failure-mode decision} only once the call has
     * failed.
     */
    Uni<RateLimitDecision> attempt(String key, long cost, RateLimitPolicy policy) {
        return guarded(key, cost, policy);
    }

    /**
     * The {@code store-failure-mode} decision for {@code cost} permits, without calling the store: the local bucket
     * under {@code local}, admission under {@code open}, an unavailable outcome under {@code closed}.
     */
    Uni<Granted> fallback(String key, long cost, RateLimitPolicy policy) {
        return switch (failureMode) {
            case LOCAL -> local.tryConsume(key, cost, policy)
                    .map(new Function<>() {
                        @Override
                        public Granted apply(RateLimitDecision decision) {
                            return new Granted(decision, Source.LOCAL_FALLBACK);
                        }
                    });
            case OPEN -> Uni.createFrom().item(new Granted(
                    RateLimitDecision.allowed(policy.burst()), Source.OPEN_FALLBACK));
            case CLOSED -> Uni.createFrom().item(Granted.unavailable(breakerDelay));
        };
    }

    /**
     * The only way to the delegate: the concurrency limit and the inner deadline, inside the guard.
     */
    private Uni<RateLimitDecision> guarded(String key, long cost, RateLimitPolicy policy) {
        return Uni.createFrom().deferred(new Supplier<>() {
            @Override
            public Uni<? extends RateLimitDecision> get() {
                try {
                    return guard.call(new Callable<>() {
                        @Override
                        public Uni<RateLimitDecision> call() {
                            return admit(key, cost, policy);
                        }
                    }, DECISION);
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }
            }
        });
    }

    /**
     * Enforces {@code maxConcurrentRequests} with no queue. SmallRye Fault Tolerance 7.0.0 refuses a bulkhead
     * without a queue ({@code Guard.Builder.BulkheadBuilder.queueSize} must be at least 1), and a queued call would wait
     * outside the inner deadline; so the limit is enforced here instead of by the guard's bulkhead. The rejection
     * happens inside the circuit breaker, which counts it as a failure, as it would a bulkhead rejection; the guard
     * therefore publishes no bulkhead metrics or events for these stores.
     * <p>
     * A {@linkplain #POOL_REFUSAL pool refusal} while the breaker is half-open fails with an exception the breaker does
     * not skip, so that the refused trial call reopens the breaker instead of closing it as a success: a pool that is
     * full right after an outage more likely holds calls to a store that is still hung than to a healthy one.
     */
    private Uni<RateLimitDecision> admit(String key, long cost, RateLimitPolicy policy) {
        if (inFlight.incrementAndGet() > maxConcurrentRequests) {
            inFlight.decrementAndGet();
            return Uni.createFrom().failure(new BulkheadException(
                    "Rate limit store '" + name + "' already has the most calls in flight (" + maxConcurrentRequests
                            + ")"));
        }
        return withInnerDeadline(key, cost, policy)
                .onFailure(new Predicate<>() {
                    @Override
                    public boolean test(Throwable failure) {
                        return degraded() && CustomizableCircuitBreaker.causedBy(failure, POOL_REFUSAL);
                    }
                })
                .transform(new Function<>() {
                    @Override
                    public Throwable apply(Throwable failure) {
                        // not the cause, which the breaker would find and skip
                        IllegalStateException trialFailed = new IllegalStateException(
                                "Rate limit store '" + name
                                        + "': the connection pool refused the trial call of the half-open"
                                        + " circuit breaker: " + failure);
                        trialFailed.addSuppressed(failure);
                        return trialFailed;
                    }
                })
                .onTermination().invoke(new Runnable() {
                    @Override
                    public void run() {
                        inFlight.decrementAndGet();
                    }
                });
    }

    /**
     * Bounds the delegate call by the store timeout on the rate limit clock, cancelling the call when it expires. The
     * guard's own timeout does not terminate the delegate's {@code Uni} (SmallRye subscribes and discards the
     * subscription), so without this a call that never completes would hold its slot forever.
     */
    private Uni<RateLimitDecision> withInnerDeadline(String key, long cost, RateLimitPolicy policy) {
        return Uni.createFrom().emitter(new Consumer<>() {
            @Override
            public void accept(UniEmitter<? super RateLimitDecision> em) {
                AtomicBoolean settled = new AtomicBoolean();
                AtomicReference<Cancellable> upstream = new AtomicReference<>();
                RateLimitClock.Cancellable deadline = clock.schedule(timeout, new Runnable() {
                    @Override
                    public void run() {
                        if (settled.compareAndSet(false, true)) {
                            cancel(upstream);
                            em.fail(new TimeoutException(
                                    "Rate limit store '" + name + "' did not answer within " + timeout.toMillis()
                                            + " ms"));
                        }
                    }
                });
                em.onTermination(new Runnable() {
                    @Override
                    public void run() {
                        deadline.cancel();
                        // cancelled from downstream: nothing has settled, so stop the delegate call too
                        if (!settled.get()) {
                            cancel(upstream);
                        }
                    }
                });
                // The sealed decision type permits only Allowed or Rejected; a null item is a failed call,
                // which the failure mode decides, instead of the store bypassing it.
                Cancellable subscription = Uni.createFrom().deferred(new Supplier<Uni<? extends RateLimitDecision>>() {
                    @Override
                    public Uni<? extends RateLimitDecision> get() {
                        return delegate.tryConsume(key, cost, policy);
                    }
                }).onItem().ifNull().failWith(new Supplier<>() {
                    @Override
                    public Throwable get() {
                        return new IllegalStateException("Rate limit store '" + name
                                + "' returned null instead of Allowed or Rejected");
                    }
                }).subscribe().with(
                        new Consumer<>() {
                            @Override
                            public void accept(RateLimitDecision item) {
                                if (settled.compareAndSet(false, true)) {
                                    em.complete(item);
                                }
                            }
                        },
                        new Consumer<>() {
                            @Override
                            public void accept(Throwable failure) {
                                if (settled.compareAndSet(false, true)) {
                                    em.fail(failure);
                                }
                            }
                        });
                upstream.set(subscription);
                // the deadline may have fired before the subscription was recorded; cancelling a call that has already
                // answered does nothing
                if (settled.get()) {
                    subscription.cancel();
                }
            }
        });
    }

    private static void cancel(AtomicReference<Cancellable> upstream) {
        Cancellable subscription = upstream.get();
        if (subscription != null) {
            subscription.cancel();
        }
    }
}

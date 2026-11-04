package io.quarkus.http.ratelimit.store;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.quarkus.http.ratelimit.runtime.internal.store.LocalRateLimitStore;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * A store whose behaviour tests switch at will: it succeeds through an inner {@link LocalRateLimitStore}, throws,
 * answers late, or never answers.
 */
@Singleton
@Identifier("switchable")
public class SwitchableTestStore implements RateLimitStore {

    public sealed interface Behaviour {
    }

    public record Succeed() implements Behaviour {
    }

    public record Throw() implements Behaviour {
    }

    /**
     * Fails every call with {@code failure}.
     */
    public record FailWith(RuntimeException failure) implements Behaviour {
    }

    public record Delay(Duration delay) implements Behaviour {
    }

    public record NeverComplete() implements Behaviour {
    }

    /**
     * Fails the first call made under it, then succeeds.
     */
    public record FailOnce() implements Behaviour {
    }

    public static final AtomicReference<Behaviour> behaviour = new AtomicReference<>(new Succeed());
    /**
     * Calls with a cost above 0 that reached this store.
     */
    public static final AtomicInteger calls = new AtomicInteger();
    /**
     * The costs of the calls above 0, in order.
     */
    public static final List<Long> costs = new CopyOnWriteArrayList<>();
    /**
     * Cost 0 calls (probes) that reached this store.
     */
    public static final AtomicInteger probeCalls = new AtomicInteger();
    /**
     * Never-completing calls whose subscription was cancelled.
     */
    public static final AtomicInteger cancelled = new AtomicInteger();
    private static final AtomicBoolean failedOnce = new AtomicBoolean();

    private final LocalRateLimitStore inner;
    private final Vertx vertx;

    @Inject
    public SwitchableTestStore(RateLimitClock clock, Vertx vertx) {
        this.inner = new LocalRateLimitStore(clock);
        this.vertx = vertx;
    }

    public static void reset() {
        behaviour.set(new Succeed());
        calls.set(0);
        costs.clear();
        probeCalls.set(0);
        cancelled.set(0);
        failedOnce.set(false);
    }

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        (cost == 0 ? probeCalls : calls).incrementAndGet();
        if (cost > 0) {
            costs.add(cost);
        }
        return switch (behaviour.get()) {
            case Succeed ignored -> inner.tryConsume(key, cost, policy);
            case Throw ignored -> Uni.createFrom().failure(new IllegalStateException("the store is down"));
            case FailWith failWith -> Uni.createFrom().failure(failWith.failure());
            case Delay delay -> Uni.createFrom().emitter(em -> vertx.setTimer(delay.delay().toMillis(),
                    ignored -> inner.tryConsume(key, cost, policy).subscribe().with(em::complete, em::fail)));
            // the emitter never completes, so its only termination is a cancellation
            case NeverComplete ignored -> Uni.createFrom().emitter(em -> em.onTermination(cancelled::incrementAndGet));
            case FailOnce ignored -> failedOnce.compareAndSet(false, true)
                    ? Uni.createFrom().failure(new IllegalStateException("the store is down once"))
                    : inner.tryConsume(key, cost, policy);
        };
    }
}

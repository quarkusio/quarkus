package io.quarkus.http.ratelimit.runtime.internal.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;
import io.quarkus.http.ratelimit.runtime.internal.store.GuardedRateLimitStore.Granted;
import io.quarkus.http.ratelimit.runtime.internal.store.GuardedRateLimitStore.Source;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;

/**
 * The concurrency limit of {@link GuardedRateLimitStore}: a slot is taken per delegate call and returned on every
 * termination, and a call beyond the limit fails at once.
 */
class GuardedRateLimitStoreTest {

    static final Duration TIMEOUT = Duration.ofMillis(200);
    static final RateLimitPolicy POLICY = new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION, 5,
            Duration.ofMinutes(1), 5, 100);
    static final RateLimitDecision.Allowed ALLOWED = RateLimitDecision.allowed(4);

    final MutableClock clock = new MutableClock();
    final AtomicInteger inFlight = new AtomicInteger();
    final List<UniEmitter<? super RateLimitDecision>> pending = new ArrayList<>();
    final AtomicInteger terminated = new AtomicInteger();
    final AtomicInteger delegateCalls = new AtomicInteger();
    GuardedRateLimitStore store;

    @BeforeEach
    void setUp() {
        // every delegate call stays pending until the test completes, fails or cancels it
        RateLimitStore delegate = (key, cost, policy) -> {
            delegateCalls.incrementAndGet();
            return Uni.createFrom().emitter(em -> {
                pending.add(em);
                em.onTermination(terminated::incrementAndGet);
            });
        };
        store = new GuardedRateLimitStore("s", new PassThroughGuard(),
                new AtomicReference<>(CircuitBreakerState.CLOSED), Duration.ofSeconds(10), inFlight, 2, delegate, TIMEOUT,
                FailureMode.OPEN, new LocalRateLimitStore(clock), clock);
    }

    /**
     * {@link GuardedRateLimitStore#tryConsume} with the authority of the grant: the guarded call, then the failure-mode
     * fallback.
     */
    UniAssertSubscriber<Granted> consume() {
        return store.attempt("k", 1, POLICY)
                .map(decision -> new Granted(decision, Source.STORE))
                .onFailure().recoverWithUni(failure -> store.fallback("k", 1, POLICY))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
    }

    @Test
    void slotIsReturnedOnItem() {
        UniAssertSubscriber<Granted> result = consume();
        assertThat(inFlight).hasValue(1);

        pending.getFirst().complete(ALLOWED);

        assertThat(inFlight).hasValue(0);
        assertThat(result.getItem()).isEqualTo(new Granted(ALLOWED, Source.STORE));
        assertThat(terminated).hasValue(1);
    }

    @Test
    void slotIsReturnedOnFailure() {
        UniAssertSubscriber<Granted> result = consume();
        assertThat(inFlight).hasValue(1);

        pending.getFirst().fail(new IllegalStateException("down"));

        assertThat(inFlight).hasValue(0);
        assertThat(result.getItem()).isEqualTo(new Granted(RateLimitDecision.allowed(5),
                Source.OPEN_FALLBACK));
    }

    @Test
    void slotIsReturnedOnCancellation() {
        UniAssertSubscriber<Granted> result = consume();
        assertThat(inFlight).hasValue(1);

        result.cancel();

        assertThat(inFlight).hasValue(0);
        assertThat(terminated).hasValue(1);
    }

    @Test
    void slotIsReturnedWhenTheDeadlineExpires() {
        UniAssertSubscriber<Granted> result = consume();
        clock.advance(TIMEOUT.minusMillis(1));
        assertThat(inFlight).hasValue(1);
        assertThat(terminated).hasValue(0);

        clock.advance(Duration.ofMillis(1));

        assertThat(inFlight).hasValue(0);
        // nothing completed the delegate call, so its termination is the cancellation by the deadline
        assertThat(terminated).hasValue(1);
        assertThat(result.getItem().source()).isEqualTo(Source.OPEN_FALLBACK);
    }

    @Test
    void callsBeyondTheLimitFailAtOnceWithoutTakingASlot() {
        consume();
        consume();
        assertThat(inFlight).hasValue(2);

        UniAssertSubscriber<Granted> third = consume();

        assertThat(third.getItem().source()).isEqualTo(Source.OPEN_FALLBACK);
        assertThat(inFlight).hasValue(2);
        assertThat(delegateCalls).hasValue(2);
    }

    @Test
    void probeTakesASlotAndHasNoFallback() {
        UniAssertSubscriber<RateLimitDecision> probe = store.probe(POLICY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        assertThat(inFlight).hasValue(1);
        assertThat(delegateCalls).hasValue(1);

        pending.getFirst().complete(ALLOWED);
        assertThat(inFlight).hasValue(0);
        assertThat(probe.getItem()).isEqualTo(ALLOWED);

        consume();
        consume();
        UniAssertSubscriber<RateLimitDecision> rejected = store.probe(POLICY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        assertThat(rejected.getFailure()).isInstanceOf(BulkheadException.class);

        clock.advance(TIMEOUT);
        UniAssertSubscriber<RateLimitDecision> timedOut = store.probe(POLICY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        clock.advance(TIMEOUT);
        assertThat(timedOut.getFailure()).isInstanceOf(TimeoutException.class);
        assertThat(inFlight).hasValue(0);
    }

    @Test
    void degradedFollowsTheBreakerState() {
        AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        GuardedRateLimitStore guarded = new GuardedRateLimitStore("s",
                new PassThroughGuard(), state, Duration.ofSeconds(10), inFlight, 2,
                (key, cost, policy) -> Uni.createFrom().item(ALLOWED),
                TIMEOUT, FailureMode.OPEN, new LocalRateLimitStore(clock), clock);
        assertThat(guarded.degraded()).isFalse();
        state.set(CircuitBreakerState.OPEN);
        assertThat(guarded.degraded()).isTrue();
        state.set(CircuitBreakerState.HALF_OPEN);
        assertThat(guarded.degraded()).isTrue();
        state.set(CircuitBreakerState.CLOSED);
        assertThat(guarded.degraded()).isFalse();
    }

    /**
     * A store decides with {@code Allowed} or {@code Rejected} and signals a failure with a failed {@code Uni}. An
     * {@code Unavailable} or a {@code null} from it is a failed call, so the configured failure mode decides, here
     * {@code open}, instead of the store bypassing it.
     */
    @Test
    void unavailableOrNullFromTheStoreIsAFailedCall() {
        for (RateLimitDecision fromStore : new RateLimitDecision[] { RateLimitDecision.unavailable(Duration.ofSeconds(1)),
                null }) {
            GuardedRateLimitStore guarded = new GuardedRateLimitStore("s", new PassThroughGuard(),
                    new AtomicReference<>(CircuitBreakerState.CLOSED), Duration.ofSeconds(10), inFlight, 2,
                    (key, cost, policy) -> Uni.createFrom().item(fromStore),
                    TIMEOUT, FailureMode.OPEN, new LocalRateLimitStore(clock), clock);
            Granted granted = guarded.grant("k", 1, POLICY).await().indefinitely();
            assertThat(granted.source()).as(String.valueOf(fromStore)).isEqualTo(Source.OPEN_FALLBACK);
            assertThat(granted.decision()).isInstanceOf(RateLimitDecision.Allowed.class);
        }
    }

    /**
     * A decision without a {@code Retry-After} cannot be built: a store that tries fails its own call, which the
     * failure mode decides, instead of failing the request later.
     */
    @Test
    void rejectionWithoutRetryAfterIsAFailedCall() {
        GuardedRateLimitStore guarded = new GuardedRateLimitStore("s", new PassThroughGuard(),
                new AtomicReference<>(CircuitBreakerState.CLOSED), Duration.ofSeconds(10), inFlight, 2,
                (key, cost, policy) -> Uni.createFrom().item(RateLimitDecision.rejected(null)),
                TIMEOUT, FailureMode.OPEN, new LocalRateLimitStore(clock), clock);
        Granted granted = guarded.grant("k", 1, POLICY).await().indefinitely();
        assertThat(granted.source()).isEqualTo(Source.OPEN_FALLBACK);
        assertThatThrownBy(() -> RateLimitDecision.unavailable(null)).isInstanceOf(NullPointerException.class);
    }
}

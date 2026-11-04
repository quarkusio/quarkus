package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.arc.All;
import io.quarkus.http.ratelimit.RateLimitLocalStoreCustomizer;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.RateLimitStoreGuardCustomizer;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.Guard;

/**
 * Selects the store of each rule: the rule's {@code store}, else {@code quarkus.http.rate-limit.store}. A non-local
 * store is a {@link RateLimitStore} bean annotated {@code @Identifier("<name>")}, wrapped in a
 * {@link GuardedRateLimitStore} with the timeout and failure mode of the store; the guard and its {@link StoreProbe} are
 * created once per store and shared, and every rule gets a {@link RefusalCachingRateLimitStore} over it.
 * <p>
 * The registry keeps every probe it creates and closes them in {@link #close()}, since CDI never calls lifecycle
 * methods on objects created with {@code new}.
 */
@Singleton
public class StoreRegistry {

    private static final Logger LOG = Logger.getLogger(StoreRegistry.class);

    private static final String LOCAL = "local";
    private static final String REDIS = "redis";
    private static final int MAX_CONCURRENT_REQUESTS = 256;
    private static final int BREAKER_REQUEST_VOLUME_THRESHOLD = 20;
    private static final double BREAKER_FAILURE_RATIO = 0.5;
    // the MicroProfile Fault Tolerance default
    private static final Duration BREAKER_DELAY = Duration.ofSeconds(5);

    private final HttpRateLimitRuntimeConfig config;
    private final LocalRateLimitStore local;
    private final Instance<RateLimitStore> stores;
    private final RateLimitClock clock;
    private final List<RateLimitStoreGuardCustomizer> guardCustomizers;
    private final List<RateLimitLocalStoreCustomizer> localCustomizers;
    private final Map<String, SharedGuard> guards = new HashMap<>();
    private final Map<String, StoreProbe> probes = new ConcurrentHashMap<>();

    /**
     * @param breakerDelay the delay of the breaker, after the customizers
     */
    private record SharedGuard(Guard guard, AtomicReference<CircuitBreakerState> breakerState, AtomicInteger inFlight,
            Duration breakerDelay) {
    }

    @Inject
    public StoreRegistry(HttpRateLimitRuntimeConfig config,
            @Identifier(LOCAL) LocalRateLimitStore local,
            @Any Instance<RateLimitStore> stores,
            RateLimitClock clock,
            @All List<RateLimitStoreGuardCustomizer> guardCustomizers,
            @All List<RateLimitLocalStoreCustomizer> localCustomizers) {
        this.config = config;
        this.local = local;
        this.stores = stores;
        this.clock = clock;
        this.guardCustomizers = guardCustomizers.stream().sorted().toList();
        this.localCustomizers = localCustomizers.stream().sorted().toList();
    }

    /**
     * Selects the store of a rule. Builds the in-memory caches the rule uses now, so that the
     * {@link RateLimitLocalStoreCustomizer}s run at startup.
     *
     * @param policy the policy of the rule
     * @return the local store, else the named store wrapped in its guard and in a refusal cache
     */
    public RateLimitStore forRule(DefaultRateLimitPolicy policy, RateLimitRuleConfig rule) {
        String name = rule.store();
        // the local store does no I/O and is never guarded; rules on it keep the raw instance
        if (LOCAL.equals(name)) {
            local.prepare(policy);
            return local;
        }
        FailureMode failureMode = failureMode(name);
        if (failureMode == FailureMode.LOCAL) {
            local.prepare(policy);
        }
        RateLimitStore delegate = stores.select(Identifier.Literal.of(name)).get();
        SharedGuard shared = guards.get(name);
        if (shared == null) {
            shared = newGuard(name);
            guards.put(name, shared);
        }
        GuardedRateLimitStore guarded = new GuardedRateLimitStore(name, shared.guard, shared.breakerState,
                shared.breakerDelay, shared.inFlight, MAX_CONCURRENT_REQUESTS, delegate, timeout(name), failureMode,
                local, clock);
        if (!probes.containsKey(name)) {
            probes.put(name, new StoreProbe(guarded, policy, clock, shared.breakerDelay));
        }
        Consumer<Caffeine<Object, Object>> customizer = new Consumer<>() {
            @Override
            public void accept(Caffeine<Object, Object> builder) {
                KeyCaches.customize(localCustomizers, policy.rule(), builder);
            }
        };
        return new RefusalCachingRateLimitStore(guarded, policy, clock, customizer);
    }

    @PreDestroy
    void close() {
        for (StoreProbe probe : probes.values()) {
            probe.close();
        }
    }

    /**
     * @return {@code quarkus.http.rate-limit.redis.timeout} for the built-in {@code redis} store, if set, else
     *         {@code quarkus.http.rate-limit.store-timeout}
     */
    private Duration timeout(String store) {
        if (REDIS.equals(store) && config.redis().timeout().isPresent()) {
            return config.redis().timeout().get();
        }
        return config.storeTimeout();
    }

    /**
     * @return {@code quarkus.http.rate-limit.redis.failure-mode} for the built-in {@code redis} store, if set, else
     *         {@code quarkus.http.rate-limit.store-failure-mode}
     */
    private FailureMode failureMode(String store) {
        if (REDIS.equals(store) && config.redis().failureMode().isPresent()) {
            return config.redis().failureMode().get();
        }
        return config.storeFailureMode();
    }

    /**
     * One untyped guard per store, shared by every rule on that store. It has no bulkhead: see
     * {@link GuardedRateLimitStore}, which enforces the concurrency limit itself. It has no retry either,
     * which would add latency to the request path. Its timeout is a backstop only; the inner deadline in
     * {@link GuardedRateLimitStore} is the real store timeout.
     * <p>
     * A {@linkplain GuardedRateLimitStore#POOL_REFUSAL pool refusal} is never counted as a failure by the breaker,
     * whatever its customizers set.
     * <p>
     * The {@link RateLimitStoreGuardCustomizer}s tune its breaker, after the fixed defaults and before it is built.
     */
    private SharedGuard newGuard(String name) {
        AtomicReference<CircuitBreakerState> breakerState = new AtomicReference<>(CircuitBreakerState.CLOSED);
        Guard.Builder builder = Guard.create()
                .withDescription("http-rate-limit-store-" + name)
                .withTimeout().duration(timeout(name).toMillis() + 1_000, ChronoUnit.MILLIS).done();
        CustomizableCircuitBreaker breaker = new CustomizableCircuitBreaker(builder.withCircuitBreaker(),
                GuardedRateLimitStore.POOL_REFUSAL);
        breaker.requestVolumeThreshold(BREAKER_REQUEST_VOLUME_THRESHOLD)
                .failureRatio(BREAKER_FAILURE_RATIO)
                .delay(BREAKER_DELAY.toMillis(), ChronoUnit.MILLIS);
        for (RateLimitStoreGuardCustomizer customizer : guardCustomizers) {
            customizer.customize(name, breaker);
        }
        Guard guard = breaker.complete(new Consumer<>() {
            @Override
            public void accept(CircuitBreakerState state) {
                CircuitBreakerState previous = breakerState.getAndSet(state);
                switch (state) {
                    // WARN once per outage: a reopen after a failed probe is not a new entry into degraded mode
                    case OPEN -> {
                        if (previous == CircuitBreakerState.CLOSED) {
                            LOG.warnf("Rate limit store '%s' is degraded (circuit breaker open);"
                                    + " store-failure-mode=%s applies until it recovers", name,
                                    failureMode(name).name().toLowerCase(Locale.ROOT));
                        } else {
                            LOG.debugf("Rate limit store '%s' is still degraded; the probe failed", name);
                        }
                        // the probe is created at startup with the first rule on the store, before any call
                        StoreProbe probe = probes.get(name);
                        if (probe != null) {
                            probe.opened();
                        }
                    }
                    case CLOSED -> LOG.infof("Rate limit store '%s' recovered", name);
                    case HALF_OPEN -> {
                    }
                }
            }
        })
                .build();
        return new SharedGuard(guard, breakerState, new AtomicInteger(), breaker.delay());
    }
}

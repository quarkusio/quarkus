package io.quarkus.http.ratelimit.micrometer.runtime.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.rules.RateLimitRules;
import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;
import io.quarkus.http.ratelimit.runtime.internal.store.LeasingRateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.store.LeasingRateLimitStore.Overdraft;
import io.quarkus.http.ratelimit.runtime.internal.store.LocalRateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.RateLimitOutcomeListener;
import io.smallrye.common.annotation.Identifier;

/**
 * Publishes the decisions of the rate limit rules as Micrometer meters.
 * <p>
 * Meters are tagged by rule, phase and result only; keys (addresses, principals) are never used as tags, as their
 * cardinality is unbounded.
 * <p>
 * This bean is only registered when the Micrometer extension is present and
 * {@code quarkus.http.rate-limit.metrics.enabled} is {@code true}. When rate limiting itself is disabled at runtime,
 * there are no rule evaluators, so nothing is registered.
 */
@Singleton
public class HttpRateLimitMetrics implements MeterBinder, RateLimitOutcomeListener {
    public static final String REQUESTS = "http.rate.limit.requests";
    public static final String NEAR_LIMIT = "http.rate.limit.near.limit";
    public static final String RETRY_AFTER = "http.rate.limit.retry.after";
    public static final String KEYS = "http.rate.limit.keys";
    public static final String LEASE_CLAIMS = "http.rate.limit.lease.claims";
    public static final String LEASE_STRANDED = "http.rate.limit.lease.stranded";
    public static final String LEASE_OVERDRAFT = "http.rate.limit.lease.overdraft";
    public static final String LEASE_OVERDRAFT_UNRECONCILED = "http.rate.limit.lease.overdraft.unreconciled";
    public static final String LEASE_FALLBACK_DISCARDED = "http.rate.limit.lease.fallback.discarded";

    // an allowed request with fewer than this share of the burst left counts as near the limit
    private static final double NEAR_LIMIT_SHARE = 0.2;

    private final LocalRateLimitStore local;
    private final Map<RuleEvaluator, Counts> counts = new IdentityHashMap<>();

    @Inject
    public HttpRateLimitMetrics(RateLimitRules rules, @Identifier("local") LocalRateLimitStore local) {
        this.local = local;
        for (RateLimitPhase phase : RateLimitPhase.values()) {
            for (RuleEvaluator evaluator : rules.evaluators(phase)) {
                counts.put(evaluator, new Counts());
            }
        }
    }

    @Override
    public void allowed(RuleEvaluator evaluator, RateLimitPolicy policy, long remaining) {
        Counts c = counts.get(evaluator);
        c.allowed.increment();
        // the key's burst: a limit resolver can give it another one than the rule's
        if (remaining < NEAR_LIMIT_SHARE * policy.burst()) {
            c.nearLimit.increment();
        }
    }

    @Override
    public void rejected(RuleEvaluator evaluator, String key, long retryAfterSeconds) {
        Counts c = counts.get(evaluator);
        c.rejected.increment();
        DistributionSummary summary = c.retryAfter;
        if (summary != null) {
            summary.record(retryAfterSeconds);
        }
    }

    @Override
    public void unavailable(RuleEvaluator evaluator) {
        counts.get(evaluator).unavailable.increment();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Map.Entry<RuleEvaluator, Counts> entry : counts.entrySet()) {
            RuleEvaluator evaluator = entry.getKey();
            Counts c = entry.getValue();
            Tags tags = Tags.of("rule", evaluator.rule(), "phase", evaluator.phase().configName());
            counter(registry, REQUESTS, tags.and("result", "allowed"), c.allowed,
                    "Number of requests admitted by a rate limit rule");
            counter(registry, REQUESTS, tags.and("result", "rejected"), c.rejected,
                    "Number of requests rejected by a rate limit rule");
            c.retryAfter = DistributionSummary.builder(RETRY_AFTER)
                    .description("The Retry-After of the requests rejected by a rate limit rule")
                    .baseUnit("seconds")
                    .tags(tags)
                    .register(registry);
            // the local store never fails
            if (evaluator.store() != local || evaluator.configuration().limitResolver().isPresent()
                    || evaluator.configuration().key().startsWith("custom:")) {
                counter(registry, REQUESTS, tags.and("result", "unavailable"), c.unavailable,
                        "Number of requests answered with 503 because a rate limit could not be evaluated");
            }
            counter(registry, NEAR_LIMIT, tags, c.nearLimit,
                    "Number of admitted requests that left less than a fifth of the burst");
            // a rule on another store holds keys in this replica only in its local fallback; no gauge for those
            if (evaluator.store() == local) {
                Gauge.builder(KEYS, evaluator.configuration().id(), new ToDoubleFunction<>() {
                    @Override
                    public double applyAsDouble(String id) {
                        return local.trackedKeys(id);
                    }
                })
                        .description("Number of keys the rule holds in this replica's memory")
                        .tags(tags)
                        .register(registry);
            } else if (evaluator.store() instanceof LeasingRateLimitStore lease) {
                Gauge.builder(KEYS, lease, new ToDoubleFunction<>() {
                    @Override
                    public double applyAsDouble(LeasingRateLimitStore l) {
                        return l.trackedKeys();
                    }
                })
                        .description("Number of keys the rule holds in this replica's memory")
                        .tags(tags)
                        .register(registry);
                leaseMeters(registry, tags, lease);
            }
        }
    }

    private static void leaseMeters(MeterRegistry registry, Tags tags, LeasingRateLimitStore lease) {
        for (LeaseCounter counter : LeaseCounter.values()) {
            FunctionCounter.builder(counter.meterName, lease, counter)
                    .description(counter.description)
                    .tags(tags.and(counter.tags))
                    .register(registry);
        }
        for (Overdraft category : Overdraft.values()) {
            FunctionCounter.builder(LEASE_OVERDRAFT, lease, new ToDoubleFunction<>() {
                @Override
                public double applyAsDouble(LeasingRateLimitStore l) {
                    return l.overdraft(category);
                }
            })
                    .description("Requests a lease rule with max-overdraft admitted without waiting for the store, by"
                            + " result: admitted counts each once; settled, settled_fallback, settled_late and forgiven"
                            + " say how it was paid for, or that it never was; unreconciled that the replica lost track"
                            + " of it before the store answered, in which case it is counted again once a late answer"
                            + " tells")
                    .tags(tags.and("result", category.tag()))
                    .register(registry);
        }
        Gauge.builder(LEASE_OVERDRAFT_UNRECONCILED, lease, new ToDoubleFunction<>() {
            @Override
            public double applyAsDouble(LeasingRateLimitStore l) {
                return l.unreconciledOutstanding();
            }
        })
                .description("Requests admitted on overdraft whose payment is still unknown: the replica lost track of"
                        + " them before the store answered the claim that pays for them")
                .tags(tags)
                .register(registry);
    }

    private static void counter(MeterRegistry registry, String name, Tags tags, LongAdder adder, String description) {
        FunctionCounter.builder(name, adder, new ToDoubleFunction<>() {
            @Override
            public double applyAsDouble(LongAdder a) {
                return a.sum();
            }
        })
                .description(description)
                .tags(tags)
                .register(registry);
    }

    private enum LeaseCounter implements ToDoubleFunction<LeasingRateLimitStore> {
        CLAIMS_GRANTED(LEASE_CLAIMS, "Number of lease claims the store granted", Tags.of("result", "granted")) {
            @Override
            public double applyAsDouble(LeasingRateLimitStore l) {
                return l.claims(true);
            }
        },
        CLAIMS_REFUSED(LEASE_CLAIMS, "Number of lease claims the store refused", Tags.of("result", "refused")) {
            @Override
            public double applyAsDouble(LeasingRateLimitStore l) {
                return l.claims(false);
            }
        },
        STRANDED(LEASE_STRANDED, "Number of store-granted permits this replica never served", Tags.empty()) {
            @Override
            public double applyAsDouble(LeasingRateLimitStore l) {
                return l.stranded();
            }
        },
        FALLBACK_DISCARDED(LEASE_FALLBACK_DISCARDED, "Number of locally granted fallback permits dropped unserved",
                Tags.empty()) {
            @Override
            public double applyAsDouble(LeasingRateLimitStore l) {
                return l.fallbackDiscarded();
            }
        };

        private final String meterName;
        private final String description;
        private final Tags tags;

        LeaseCounter(String meterName, String description, Tags tags) {
            this.meterName = meterName;
            this.description = description;
            this.tags = tags;
        }
    }

    private static final class Counts {
        final LongAdder allowed = new LongAdder();
        final LongAdder rejected = new LongAdder();
        final LongAdder unavailable = new LongAdder();
        final LongAdder nearLimit = new LongAdder();
        volatile DistributionSummary retryAfter;
    }
}

package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.util.Optional;
import java.util.function.Function;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A configured rule at runtime, built once at startup by {@link RateLimitRules}. {@link #evaluate} applies the rule to
 * one request: it resolves the request's key and asks the rule's store whether that key may make one more request.
 * The returned {@link Outcome} carries the store's decision, which the filter turns into the response.
 */
public final class RuleEvaluator {

    private final RateLimitPolicy policy;
    private final KeyResolution keyResolution;
    private final RateLimitStore store;
    private final String storeName;

    /**
     * @param storeName the effective store name: the rule's {@code store}, else {@code quarkus.http.rate-limit.store}
     */
    RuleEvaluator(DefaultRateLimitPolicy policy, RateLimitKeyResolver resolver,
            RateLimitStore store, String storeName, ThrottledLoggers loggers) {
        this.policy = policy;
        this.keyResolution = new KeyResolution(policy, resolver, loggers);
        this.store = store;
        this.storeName = storeName;
    }

    public String rule() {
        return policy.rule();
    }

    public RateLimitPhase phase() {
        return policy.phase();
    }

    /**
     * @return the configured policy
     */
    public RateLimitPolicy policy() {
        return policy;
    }

    /**
     * @return the store instance that makes the rule's decisions
     */
    public RateLimitStore store() {
        return store;
    }

    /**
     * @return the effective store name: the rule's {@code store}, else {@code quarkus.http.rate-limit.store}
     */
    public String storeName() {
        return storeName;
    }

    /**
     * Resolves key and policy through {@link KeyResolution}, then consults the store, whose stage is not bounded here.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Uni<Outcome> evaluate(RoutingContext ctx, Optional<SecurityIdentity> identity) {
        return keyResolution.resolve(ctx, identity).onItem()
                .transformToUni(new Function<>() {
                    @Override
                    public Uni<? extends Outcome> apply(KeyResolution.Result result) {
                        return switch (result) {
                            case KeyResolution.Skip skip -> Uni.createFrom()
                                    .item(new Outcome(RuleEvaluator.this, null, null, Outcome.Kind.SKIPPED));
                            case KeyResolution.Resolved resolved -> store.tryConsume(resolved.key(), 1, policy)
                                    .map(new Function<>() {
                                        @Override
                                        public Outcome apply(RateLimitDecision decision) {
                                            return new Outcome(RuleEvaluator.this, resolved.key(), decision,
                                                    Outcome.Kind.DECIDED);
                                        }
                                    });
                        };
                    }
                });
    }
}

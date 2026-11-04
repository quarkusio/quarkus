package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitResolver;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A configured rule at runtime, built once at startup by {@link RateLimitRules}. {@link #evaluate} applies the rule to
 * one request: it resolves the request's key and asks the rule's store whether that key may make one more request.
 * The returned {@link Outcome} carries the store's decision, which the filter turns into the response.
 */
public final class RuleEvaluator {

    private final RateLimitRuleConfig rule;
    private final KeyResolution keyResolution;
    private final RateLimitStore store;
    private final String storeName;

    /**
     * @param storeName the effective store name: the rule's {@code store}, else {@code quarkus.http.rate-limit.store}
     */
    RuleEvaluator(RateLimitRuleConfig rule, RateLimitKeyResolver resolver,
            RateLimitResolver limitResolver, RateLimitStore store,
            String storeName, HttpRateLimitRuntimeConfig config, ThrottledLoggers loggers) {
        this.rule = rule;
        this.keyResolution = new KeyResolution(rule, resolver, limitResolver, config, loggers);
        this.store = store;
        this.storeName = storeName;
    }

    public String rule() {
        return rule.name();
    }

    public RateLimitPhase phase() {
        return rule.phase();
    }

    /**
     * @return the rule configuration, independent of the limit selected for a request
     */
    public RateLimitRuleConfig configuration() {
        return rule;
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
     *
     * @param onContext runs a task on the request's context: a resolver or a limit resolver may complete on any thread, and
     *        the store is called on the request's context all the same
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Uni<Outcome> evaluate(RoutingContext ctx, Optional<SecurityIdentity> identity, Executor onContext) {
        return keyResolution.resolve(ctx, identity, onContext).emitOn(onContext).onItem()
                .transformToUni(new Function<>() {
                    @Override
                    public Uni<? extends Outcome> apply(KeyResolution.Result result) {
                        return switch (result) {
                            case KeyResolution.Unavailable unavailable -> Uni.createFrom().item(
                                    Outcome.unavailable(RuleEvaluator.this, unavailable.key(), null,
                                            unavailable.reason(), Duration.ofSeconds(1)));
                            case KeyResolution.Skip skip -> Uni.createFrom()
                                    .item(new Outcome(RuleEvaluator.this, null, null, null, Outcome.Kind.SKIPPED));
                            case KeyResolution.Resolved resolved -> consume(resolved);
                        };
                    }
                });
    }

    private Uni<Outcome> consume(KeyResolution.Resolved resolved) {
        return store.tryConsume(resolved.key(), 1, resolved.policy())
                .map(new Function<RateLimitDecision, Outcome>() {
                    @Override
                    public Outcome apply(RateLimitDecision decision) {
                        return new Outcome(RuleEvaluator.this, resolved.key(), resolved.policy(), decision,
                                Outcome.Kind.DECIDED);
                    }
                });
    }
}

package io.quarkus.http.ratelimit.runtime.internal.rules;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;

/**
 * @param evaluator the rule evaluator that produced this outcome
 * @param key the resolved key; {@code null} unless {@link Kind#DECIDED}
 * @param policy the policy of the key, the rule's unless an override replaced it; {@code null} unless
 *        {@link Kind#DECIDED}
 * @param decision the store decision; {@code null} unless {@link Kind#DECIDED}
 * @param kind what happened
 */
public record Outcome(RuleEvaluator evaluator, String key, RateLimitPolicy policy, RateLimitDecision decision,
        Kind kind) {

    public enum Kind {
        /**
         * The store decided.
         */
        DECIDED,
        /**
         * The rule does not apply to this request.
         */
        SKIPPED
    }
}

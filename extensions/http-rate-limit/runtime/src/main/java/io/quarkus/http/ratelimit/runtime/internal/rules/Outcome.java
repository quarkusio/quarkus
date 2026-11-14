package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo.Reason;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;

/**
 * @param evaluator the rule evaluator that produced this outcome
 * @param key the resolved key; null if key resolution failed or the rule was skipped
 * @param policy the selected policy; null if the rule was skipped or a resolver could not supply a key or limit
 * @param decision the store decision; {@code null} unless {@link Kind#DECIDED}
 * @param kind what happened
 * @param reason the reason for an unavailable outcome, null otherwise
 * @param retryAfter the retry delay for an unavailable outcome, null otherwise
 */
public record Outcome(RuleEvaluator evaluator, String key, DefaultRateLimitPolicy policy, RateLimitDecision decision,
        Kind kind, Reason reason, Duration retryAfter) {

    public Outcome(RuleEvaluator evaluator, String key, DefaultRateLimitPolicy policy, RateLimitDecision decision, Kind kind) {
        this(evaluator, key, policy, decision, kind, null, null);
    }

    public static Outcome unavailable(RuleEvaluator evaluator, String key, DefaultRateLimitPolicy policy, Reason reason,
            Duration retryAfter) {
        return new Outcome(evaluator, key, policy, null, Kind.UNAVAILABLE, reason, retryAfter);
    }

    public enum Kind {
        /**
         * The store decided.
         */
        DECIDED,
        /** The extension could not decide and refused the request. */
        UNAVAILABLE,
        /**
         * The rule does not apply to this request.
         */
        SKIPPED
    }
}

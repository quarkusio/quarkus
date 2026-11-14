package io.quarkus.http.ratelimit.runtime.internal.telemetry;

import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;

/**
 * Internal hook told about the admissions, rejections and store failures of a rule, for metrics. Implementations are
 * CDI beans, run on the event loop and must not block.
 */
public interface RateLimitOutcomeListener {

    /**
     * The rule admitted the request.
     *
     * @param policy the policy of the key, the rule's unless a limit resolver replaced it
     * @param remaining the whole permits the store reports as left, of the policy's burst
     */
    default void allowed(RuleEvaluator evaluator, RateLimitPolicy policy, long remaining) {
    }

    /**
     * The rule rejected the request with 429.
     *
     * @param key the key that was rejected
     * @param retryAfterSeconds the value of the {@code Retry-After} header
     */
    default void rejected(RuleEvaluator evaluator, String key, long retryAfterSeconds) {
    }

    /**
     * The store of the rule failed, and its failure mode {@code closed} answered the request with 503.
     */
    default void unavailable(RuleEvaluator evaluator) {
    }
}

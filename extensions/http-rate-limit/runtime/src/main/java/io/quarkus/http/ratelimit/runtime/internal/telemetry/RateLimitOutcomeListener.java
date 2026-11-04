package io.quarkus.http.ratelimit.runtime.internal.telemetry;

import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;

/**
 * Internal hook told about the admissions and rejections of a rule, such as the dev-mode rejection log. Implementations
 * are CDI beans, run on the event loop and must not block.
 */
public interface RateLimitOutcomeListener {

    /**
     * The rule admitted the request.
     *
     * @param remaining the whole permits the store reports as left
     */
    default void allowed(RuleEvaluator evaluator, long remaining) {
    }

    /**
     * The rule rejected the request with 429.
     *
     * @param key the key that was rejected
     * @param retryAfterSeconds the value of the {@code Retry-After} header
     */
    default void rejected(RuleEvaluator evaluator, String key, long retryAfterSeconds) {
    }
}

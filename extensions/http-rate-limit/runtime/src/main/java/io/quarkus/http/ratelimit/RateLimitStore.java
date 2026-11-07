package io.quarkus.http.ratelimit;

import io.smallrye.mutiny.Uni;

/**
 * Holds the rate limit state and makes admission decisions. This is the store SPI: implement it as a CDI bean
 * annotated {@code @Identifier("<name>")} and select it with {@code quarkus.http.rate-limit.store} or
 * {@code quarkus.http.rate-limit."rule".store}. The names {@code local} and {@code redis} are taken by the built-in
 * stores.
 * <p>
 * Implementations run on the event loop and must not block. The store may be called from several event loops
 * concurrently, so implementations must be thread-safe.
 * <p>
 * The policy of a call can differ for the same key, when a {@link RateLimitOverride} replaces the limit: keep the state
 * of a key per {@link RateLimitPolicy#id()} and key, not per policy value.
 * <p>
 * There is deliberately no unconditional debit. Leasing needs only the conditional operation, and a
 * {@code cost} above one is what makes a lease a single call.
 */
public interface RateLimitStore {

    /**
     * Admission check: consumes {@code cost} permits only if they are available. Runs on the event loop; must not
     * block.
     * <p>
     * A rejection must leave the state unchanged.
     * <p>
     * A {@code cost} in {@code [1, policy.burst()]} consumes. A {@code cost} of {@code 0} is a <em>probe</em> that must
     * write nothing: its decision is that of a cost-1 call ({@link RateLimitDecision.Allowed} iff one permit is
     * available, {@link RateLimitDecision.Rejected} with the wait for one permit), except that
     * {@link RateLimitDecision.Allowed#remaining()} reports the permits available now, unchanged (a fresh burst-5 bucket
     * probes as remaining 5). Every store, including user stores, must implement the probe; the circuit-breaking
     * decorator relies on it for recovery. A {@code cost} above {@code policy.burst()} must fail with
     * {@link IllegalArgumentException}.
     * <p>
     * {@link RateLimitDecision.Allowed#remaining()} is the store's view of the whole permits left after the call. A
     * decorator that serves from local chunks returns its best estimate of the shared remaining (the last
     * authoritative remaining plus the locally held permits); that value is an estimate.
     * {@link RateLimitDecision.Rejected#retryAfter()} is the wait until {@code cost} permits are available.
     * <p>
     * The decision is {@link RateLimitDecision.Allowed} or {@link RateLimitDecision.Rejected}. A store that cannot
     * decide signals it with a failed {@link Uni}, so that {@code store-failure-mode} decides; any other decision,
     * {@link RateLimitDecision.Unavailable} included, is treated as such a failure.
     *
     * @param key the key within the policy's rule; a key longer than 256 characters, or one that starts with
     *        {@code sha256:} or contains a brace, arrives as {@code sha256:} followed by the hex SHA-256 of the resolved key
     * @param cost the number of permits to consume, {@code 0} for a probe
     * @param policy the policy to apply
     * @return the decision
     */
    Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy);
}

package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.Objects;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;

/**
 * The limit of one rule, as resolved from configuration (and possibly a per-key resolved limit). Every policy the extension
 * hands to a store is of this type; {@link #maxKeys()} is internal and not part of {@link RateLimitPolicy}.
 * <p>
 * {@link RateLimit} rejects {@code permits < 1}, {@code burst < 1}, an
 * {@linkplain #emissionIntervalMicros()
 * emission interval} below one microsecond, a {@linkplain #burstToleranceMicros() burst tolerance} above 30 days, and
 * any arithmetic overflow, with {@link IllegalArgumentException}. Both configured and resolved quotas use that value type, so
 * Java and Lua stores see the same domain.
 *
 * @param rule the rule name
 * @param phase the phase this policy applies to
 * @param limit the validated quota; retained directly when selected by a resolver
 * @param maxKeys the maximum number of keys held in the memory of a replica for this policy: by the local store, the
 *        lease cache and the local fallback of a non-local store
 */
public record DefaultRateLimitPolicy(String rule, RateLimitPhase phase, RateLimit limit,
        long maxKeys) implements RateLimitPolicy {

    public DefaultRateLimitPolicy {
        Objects.requireNonNull(limit, "limit");
    }

    @Override
    public long permits() {
        return limit.permits();
    }

    @Override
    public Duration window() {
        return limit.window();
    }

    @Override
    public long burst() {
        return limit.burst();
    }

    /**
     * Associates a selected limit with its rule, without making the limit name part of the counter identity.
     */
    public static DefaultRateLimitPolicy of(RateLimitRuleConfig rule, RateLimit limit) {
        return new DefaultRateLimitPolicy(rule.name(), rule.phase(), limit, rule.maxKeys());
    }
}

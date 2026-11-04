package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;

/**
 * Runtime configuration of the HTTP rate limiter: the limits of the rules defined in {@link HttpRateLimitBuildTimeConfig},
 * and the settings of the stores.
 */
@ConfigMapping(prefix = "quarkus.http.rate-limit")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HttpRateLimitRuntimeConfig {

    /**
     * Whether HTTP rate limiting is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Maximum time a custom request key resolver may take. Must be positive.
     */
    @WithDefault("250ms")
    Duration keyResolverTimeout();

    /**
     * Maximum time a limit resolver may take. Must be positive.
     */
    @WithDefault("250ms")
    Duration limitResolverTimeout();

    /**
     * What happens when a custom request key resolver fails or times out: {@code reject} (the default) answers 503;
     * {@code ip} explicitly opts into counting by client address instead of the intended request key. Returning an empty key
     * deliberately skips the rule and is not a failure.
     */
    @WithDefault("reject")
    KeyResolverFailure keyResolverFailure();

    /**
     * Optional named quotas containing permits, window, and burst. Defining a quota here does not limit any requests:
     * select it with a rule's {@code limit}, or return it from a limit resolver using the injectable {@code RateLimits}
     * registry. For a quota used by only one rule, set permits and window directly on that rule instead.
     * Sharing a limit does not share counters: each rule and request key keeps its own allowance.
     */
    @ConfigDocMapKey("limit-name")
    @ConfigDocSection
    Map<String, NamedLimit> limits();

    /**
     * How often a warning that can recur on every request, such as a rule that reaches its {@code max-keys}, is logged:
     * at most once per this interval for each kind of warning. At least 1 second.
     */
    @WithDefault("1m")
    Duration warningInterval();

    /**
     * The maximum number of keys a rule holds in the memory of a replica, unless it sets its own {@code max-keys}.
     */
    @WithDefault("100000")
    long maxKeys();

    /**
     * The time budget of one call to a store other than {@code local}: the built-in {@code redis} store, unless
     * {@code quarkus.http.rate-limit.redis.timeout} is set, and every custom store. A call that takes longer fails, and
     * {@code quarkus.http.rate-limit.store-failure-mode} decides the request.
     */
    @WithDefault("100ms")
    Duration storeTimeout();

    /**
     * What happens to a request when a call to a store other than {@code local} fails, takes longer than
     * {@code quarkus.http.rate-limit.store-timeout}, or is refused because the circuit breaker of the store is open or
     * 256 calls are already in flight: {@code local} decides it with an in-memory bucket of this replica, {@code open}
     * lets it through, {@code closed} answers {@code 503 Service Unavailable} with a {@code Retry-After} between one and
     * two circuit breaker delays, chosen at random. Applies to the built-in {@code redis} store, unless
     * {@code quarkus.http.rate-limit.redis.failure-mode} is set, and to every custom store.
     */
    @WithDefault("local")
    FailureMode storeFailureMode();

    /**
     * Settings of the built-in {@code redis} store.
     */
    Redis redis();

    /**
     * Limits and operational settings for the rules defined at build time. These values are read at application startup.
     */
    @ConfigDocMapKey("rule")
    @ConfigDocSection
    @WithDefaults
    Map<String, Rule> rules();

    /**
     * The limit of a rate limit rule.
     * <p>
     * Permits refill continuously at {@code permits / window}; {@code burst} (default {@code permits}) is the most that
     * can be consumed at once; this is a token bucket, not a calendar window.
     */
    @ConfigGroup
    interface Rule {

        /**
         * Whether this rule is enabled. Can be changed when starting the built application.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Name of a quota defined under {@code quarkus.http.rate-limit.limits}, for example {@code standard} to use
         * {@code quarkus.http.rate-limit.limits.standard}. Applies that quota to every client of this rule, with a separate
         * allowance for each request key. Requires no Java resolver. Select either this, inline permits and window,
         * or a limit resolver; do not combine them. A static limit on a rule takes precedence over the globally inherited
         * resolver.
         */
        Optional<String> limit();

        /**
         * Name of a quota under {@code quarkus.http.rate-limit.limits} to use if the limit resolver fails, times out or
         * emits null. The resolver still chooses the normal quota for every request. Without a fallback the request
         * receives 503 on lookup failure. Only valid for rules using a limit resolver.
         */
        Optional<String> fallbackLimit();

        /**
         * For an inline limit, the number of permits replenished per {@code quarkus.http.rate-limit.rules."rule".window}, at
         * least 1. The interval
         * between two permits, {@code window / permits}, must be at least 1 microsecond.
         */
        Optional<Long> permits();

        /**
         * For an inline limit, the window over which {@code quarkus.http.rate-limit.rules."rule".permits} are replenished, for
         * example {@code 1m}.
         * Must be positive.
         */
        Optional<Duration> window();

        /**
         * The most permits that can be consumed at once, at least 1. Defaults to
         * {@code quarkus.http.rate-limit.rules."rule".permits}. {@code (window / permits) * burst} must not exceed 30 days.
         */
        Optional<Long> burst();

        /**
         * The maximum number of keys this rule holds in the memory of a replica: in the {@code local} store, and in
         * the local fallback of {@code store-failure-mode=local}. Defaults to
         * {@code quarkus.http.rate-limit.max-keys}.
         */
        Optional<Long> maxKeys();

    }

    /**
     * A named limit, independent of any rule or counter key.
     */
    @ConfigGroup
    interface NamedLimit {
        /**
         * Number of permits replenished per window for this named quota, at least one. The quota only takes effect when
         * selected by a rule's {@code limit} or returned by a limit resolver. Each rule and request key keeps its own
         * allowance, even when they select the same quota.
         */
        long permits();

        /**
         * Replenishment window for this named quota, for example {@code 1m}. Must be positive; the interval per permit
         * must be at least one microsecond.
         */
        Duration window();

        /**
         * Maximum permits available at once. Defaults to permits. Refilling a burst must take at most 30 days.
         */
        Optional<Long> burst();
    }

    enum KeyResolverFailure {
        IP,
        REJECT
    }

    /**
     * Settings of the built-in {@code redis} store. Its client is selected at build time by
     * {@code quarkus.http.rate-limit.redis.client-name}.
     */
    @ConfigGroup
    interface Redis {

        /**
         * The prefix of every key the built-in Redis store writes. It isolates applications that share one database.
         * A custom {@code RateLimitStore} bean receives the key without this prefix.
         */
        @WithDefault("quarkus:http-rate-limit:${quarkus.application.name:app}:")
        String keyPrefix();

        /**
         * The time budget of one call to the built-in {@code redis} store. Defaults to
         * {@code quarkus.http.rate-limit.store-timeout}, itself {@code 100ms} by default.
         */
        Optional<Duration> timeout();

        /**
         * What happens when the built-in {@code redis} store fails. Defaults to
         * {@code quarkus.http.rate-limit.store-failure-mode}, itself {@code local} by default.
         */
        Optional<FailureMode> failureMode();
    }

    enum FailureMode {
        LOCAL,
        OPEN,
        CLOSED
    }
}

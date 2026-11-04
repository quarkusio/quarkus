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
import io.smallrye.config.WithParentName;

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
     * The rate limit rules, keyed by rule name. Their limits can change at runtime; every rule defined at build time
     * must have one, and every rule here must be defined at build time.
     */
    @ConfigDocMapKey("rule")
    @ConfigDocSection
    @WithParentName
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
         * The number of permits replenished per {@code quarkus.http.rate-limit."rule".window}, at least 1. The interval
         * between two permits, {@code window / permits}, must be at least 1 microsecond.
         */
        long permits();

        /**
         * The window over which {@code quarkus.http.rate-limit."rule".permits} are replenished, for example {@code 1m}.
         * Must be positive.
         */
        Duration window();

        /**
         * The most permits that can be consumed at once, at least 1. Defaults to
         * {@code quarkus.http.rate-limit."rule".permits}. {@code (window / permits) * burst} must not exceed 30 days.
         */
        Optional<Long> burst();

        /**
         * The maximum number of keys this rule holds in the memory of a replica: in the {@code local} store.
         * Defaults to {@code quarkus.http.rate-limit.max-keys}.
         */
        Optional<Long> maxKeys();
    }
}

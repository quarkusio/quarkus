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
     * The maximum number of keys a rule holds in the memory of a replica, unless it sets its own {@code max-keys}: in
     * the {@code local} store, in the lease cache of a rule with {@code lease-size}, and in the in-memory fallback of a
     * store whose failure mode is {@code local}. When it is reached, keys are evicted, and an evicted key starts again
     * with a full burst. At least 1.
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
         * The maximum number of keys this rule holds in the memory of a replica: in the {@code local} store, in the
         * lease cache of a rule with {@code lease-size}, and in the in-memory fallback of a store whose failure mode is
         * {@code local}. When it is reached, keys are evicted, and an evicted key starts again with a full burst. At
         * least 1. Defaults to {@code quarkus.http.rate-limit.max-keys}, itself {@code 100000} by default.
         */
        Optional<Long> maxKeys();

        /**
         * When set, the rule uses lease mode: it claims chunks of up to this many permits from the store and serves
         * requests from the chunk, trading admission precision for fewer store calls. The actual chunk adapts to the
         * demand of this replica, and a chunk may be served for {@code lease-size} emission intervals
         * ({@code window / permits}). Between 1 and the burst; at most {@code permits / 20} is recommended, because
         * every replica may strand up to one chunk. Requires a store other than {@code local}. When absent, every
         * request makes one store call.
         */
        Optional<Long> leaseSize();

        /**
         * The number of requests per key that a replica may admit without holding permits, per emission interval
         * ({@code window / permits}), while it claims a chunk from the store. {@code 0} is strict: a request that finds
         * no chunk waits for the claim, at most the store timeout ({@code quarkus.http.rate-limit.store-timeout}, or
         * {@code quarkus.http.rate-limit.redis.timeout} for the {@code redis} store). Above {@code 0} is optimistic: up
         * to this many requests per emission interval are admitted at once and settled with the claim; when they are
         * used up, or for a while after the store refused a claim, requests wait as in strict mode. Admissions that the
         * store then refuses got through over the limit; they are forgiven, and counted. Between 0 and the burst; only
         * valid with {@code lease-size}.
         */
        @WithDefault("0")
        long maxOverdraft();
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

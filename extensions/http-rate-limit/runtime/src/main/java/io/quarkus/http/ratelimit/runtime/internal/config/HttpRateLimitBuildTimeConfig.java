package io.quarkus.http.ratelimit.runtime.internal.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration of the HTTP rate limiter that is fixed at build time: the paths, methods, request keys, phases, stores and
 * limit resolvers of each rule.
 * The limits of the rules are in {@link HttpRateLimitRuntimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.http.rate-limit")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HttpRateLimitBuildTimeConfig {

    /**
     * The store of every rule that does not set its own {@code store}: {@code local}, or the
     * {@code @Identifier} of a user supplied {@code RateLimitStore} bean.
     */
    @WithDefault("local")
    String store();

    /**
     * What every rule that does not set its own {@code request-key} counts requests per: every distinct key gets its own
     * {@code quarkus.http.rate-limit.rules."rule".permits} per {@code quarkus.http.rate-limit.rules."rule".window}. One of:
     * <ul>
     * <li>{@code ip}: the client address</li>
     * <li>{@code principal}: the authenticated user. Only available after authentication; a request without a
     * non-anonymous identity is keyed by its client address, like {@code ip}</li>
     * <li>{@code header:<name>}: the value of that request header, stored hashed; a request without it is keyed by its
     * client address, and every request that repeats it by one shared key</li>
     * <li>{@code global}: one key for every request a rule matches</li>
     * <li>{@code custom:<name>}: the {@code RateLimitKeyResolver} bean of the application annotated
     * {@code @Identifier("<name>")}</li>
     * </ul>
     */
    Optional<String> requestKey();

    /**
     * When every rule that does not set its own {@code phase} is evaluated: {@code pre-authentication}, before
     * authentication, so that only the request itself is known; or {@code post-authentication}, after authentication,
     * so that the {@code principal} request key is available.
     */
    Optional<RateLimitPhase> phase();

    /**
     * The limit resolver inherited by rules that do not select a resolver or a static limit of their own.
     */
    Optional<String> limitResolver();

    /**
     * The rate limit rules, keyed by name. Paths, methods and component selection are fixed at build time; quotas and
     * enablement can change at application startup.
     */
    @ConfigDocMapKey("rule")
    @ConfigDocSection
    Map<String, Rule> rules();

    /**
     * A rate limit rule: a set of paths limited together, in one phase of request processing.
     */
    @ConfigGroup
    interface Rule {

        /**
         * URI paths this rule applies to. A leading slash makes a path absolute; otherwise it is relative to
         * {@code quarkus.http.root-path}, as with HTTP permissions. Wildcards must occupy a whole segment.
         * Health probes and the Prometheus scrape endpoint are never limited. Required for every rule.
         */
        List<String> paths();

        /**
         * HTTP methods to limit. When absent all methods match. Names are upper-cased; GET also covers HEAD.
         */
        Optional<Set<String>> methods();

        /**
         * When this rule is evaluated: {@code pre-authentication}, before authentication, so that only the request
         * itself is known; or {@code post-authentication}, after authentication, so that the {@code principal} request
         * key is available. To limit a path in both phases, configure two rules. Defaults to
         * {@code quarkus.http.rate-limit.phase}; one of the two must be set.
         */
        Optional<RateLimitPhase> phase();

        /**
         * What the rule counts requests per: every distinct key gets its own
         * {@code quarkus.http.rate-limit.rules."rule".permits} per {@code quarkus.http.rate-limit.rules."rule".window}. One of:
         * <ul>
         * <li>{@code ip}: the client address</li>
         * <li>{@code principal}: the authenticated user. Only available after authentication; a request without a
         * non-anonymous identity is keyed by its client address, like {@code ip}</li>
         * <li>{@code header:<name>}: the value of that request header, stored hashed; a request without it is keyed by
         * its client address, and every request that repeats it by one shared key</li>
         * <li>{@code global}: one key for every request the rule matches</li>
         * <li>{@code custom:<name>}: the {@code RateLimitKeyResolver} bean of the application annotated
         * {@code @Identifier("<name>")}</li>
         * </ul>
         * Defaults to {@code quarkus.http.rate-limit.request-key}; one of the two must be set.
         */
        Optional<String> requestKey();

        /**
         * The store this rule uses. Defaults to {@code quarkus.http.rate-limit.store}.
         */
        Optional<String> store();

        /**
         * The {@code @Identifier} of a {@code RateLimitResolver} bean that selects the limit for each request.
         * Defaults to {@code quarkus.http.rate-limit.limit-resolver}, unless this rule supplies an inline or named
         * limit. Set {@code limit} instead to opt out of the inherited resolver.
         */
        Optional<String> limitResolver();
    }
}

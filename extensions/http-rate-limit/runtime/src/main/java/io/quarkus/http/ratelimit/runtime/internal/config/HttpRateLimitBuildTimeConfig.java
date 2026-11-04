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
import io.smallrye.config.WithParentName;

/**
 * Configuration of the HTTP rate limiter that is fixed at build time: which rules exist and what they apply to. The
 * limits of the rules are in {@link HttpRateLimitRuntimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.http.rate-limit")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HttpRateLimitBuildTimeConfig {

    /**
     * What every rule that does not set its own {@code request-key} counts requests per: every distinct key gets its own
     * {@code quarkus.http.rate-limit."rule".permits} per {@code quarkus.http.rate-limit."rule".window}. One of:
     * <ul>
     * <li>{@code ip}: the client address</li>
     * <li>{@code principal}: the authenticated user. Only available after authentication; a request without a
     * non-anonymous identity is keyed by its client address, like {@code ip}</li>
     * <li>{@code header:<name>}: the value of that request header, stored hashed; a request without it is keyed by its
     * client address, and every request that repeats it by one shared key</li>
     * <li>{@code global}: one key for every request a rule matches</li>
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
     * The rate limit rules, keyed by rule name. What a rule applies to and where its state is kept is fixed at build
     * time.
     */
    @ConfigDocMapKey("rule")
    @ConfigDocSection
    @WithParentName
    Map<String, Rule> rules();

    /**
     * A rate limit rule: a set of paths limited together, in one phase of request processing.
     */
    @ConfigGroup
    interface Rule {

        /**
         * The URI paths this rule applies to. A path starts with {@code /}; a wildcard must be a whole path segment,
         * either between two slashes or as a trailing {@code /*}. A path includes {@code quarkus.http.root-path}, and
         * request paths are matched in the same way as for HTTP permissions.
         */
        List<String> paths();

        /**
         * The HTTP methods this rule applies to, for example {@code POST} for a login path. When absent, the rule
         * applies to every method. Upper-cased at startup. {@code GET} covers {@code HEAD} too, which the REST layer
         * answers with the {@code GET} resource method.
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
         * {@code quarkus.http.rate-limit."rule".permits} per {@code quarkus.http.rate-limit."rule".window}. One of:
         * <ul>
         * <li>{@code ip}: the client address</li>
         * <li>{@code principal}: the authenticated user. Only available after authentication; a request without a
         * non-anonymous identity is keyed by its client address, like {@code ip}</li>
         * <li>{@code header:<name>}: the value of that request header, stored hashed; a request without it is keyed by
         * its client address, and every request that repeats it by one shared key</li>
         * <li>{@code global}: one key for every request the rule matches</li>
         * </ul>
         * Defaults to {@code quarkus.http.rate-limit.request-key}; one of the two must be set.
         */
        Optional<String> requestKey();
    }
}

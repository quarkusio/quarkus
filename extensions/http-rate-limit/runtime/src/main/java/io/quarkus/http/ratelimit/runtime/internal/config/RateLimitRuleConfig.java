package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.http.ratelimit.RateLimitPhase;

/**
 * One rule as the rate limiter uses it: its build time and runtime settings, with the global settings applied where
 * the rule sets none. This is the only place where the global settings are applied.
 *
 * @param name the rule name
 * @param methods the HTTP methods as configured; absent for every method
 * @param phase the phase; {@code null} when neither the rule nor {@code quarkus.http.rate-limit.phase} sets one,
 *        which fails the build
 * @param key the request key; {@code null} when neither the rule nor {@code quarkus.http.rate-limit.request-key} sets
 *        one, which fails the build
 * @param store the store name
 * @param override the {@code @Identifier} of the rule's {@code RateLimitOverride}; absent when the rule has none
 * @param burst the burst as configured; absent when it defaults to {@code permits}
 */
public record RateLimitRuleConfig(String name, List<String> paths, Optional<Set<String>> methods, RateLimitPhase phase,
        String key, String store, Optional<String> override, long permits, Duration window, Optional<Long> burst,
        long maxKeys) {

    /**
     * @return every rule, in the order of the build time configuration; only for a configuration without
     *         {@linkplain HttpRateLimitConfigValidator#validate errors}
     */
    public static List<RateLimitRuleConfig> all(HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig runtimeConfig) {
        List<RateLimitRuleConfig> rules = new ArrayList<>();
        for (String name : buildConfig.rules().keySet()) {
            rules.add(of(name, buildConfig, runtimeConfig));
        }
        return rules;
    }

    /**
     * @param name a rule that both configurations define
     */
    public static RateLimitRuleConfig of(String name, HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig runtimeConfig) {
        HttpRateLimitBuildTimeConfig.Rule build = buildConfig.rules().get(name);
        HttpRateLimitRuntimeConfig.Rule runtime = runtimeConfig.rules().get(name);
        return new RateLimitRuleConfig(name, build.paths(), build.methods(),
                phase(buildConfig, build), key(buildConfig, build), store(buildConfig, build),
                override(buildConfig, build),
                runtime.permits(), runtime.window(), runtime.burst(),
                runtime.maxKeys().orElse(runtimeConfig.maxKeys()));
    }

    /**
     * @return the phase of the rule, else {@code quarkus.http.rate-limit.phase}; {@code null} when neither is set
     */
    public static RateLimitPhase phase(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.phase().orElse(config.phase().orElse(null));
    }

    /**
     * @return the key of the rule, else {@code quarkus.http.rate-limit.request-key}; {@code null} when neither is set
     */
    public static String key(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.requestKey().orElse(config.requestKey().orElse(null));
    }

    /**
     * @return the override of the rule, else {@code quarkus.http.rate-limit.override}; absent when neither is set
     */
    public static Optional<String> override(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.override().isPresent() ? rule.override() : config.override();
    }

    /**
     * @return the store of the rule, else {@code quarkus.http.rate-limit.store}
     */
    public static String store(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.store().orElse(config.store());
    }
}

package io.quarkus.http.ratelimit.runtime.internal.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitPhase;

/**
 * One enabled rule, with shared settings applied and its static limit and fallback resolved.
 * Dynamic limits are selected per request, without changing the identity of the rule's counters.
 * This is the only place where shared defaults are applied.
 *
 * @param name the rule name
 * @param methods the HTTP methods as configured; absent for every method
 * @param phase the phase; required by build-time validation
 * @param key the request key; required by build-time validation
 * @param store the store name
 * @param limitResolver the identifier of the selected resolver; absent for a static limit
 * @param limit the inline or named limit; absent when a resolver selects the limit
 * @param fallback the named limit used if resolution fails; absent to answer 503
 */
public record RateLimitRuleConfig(String name, List<String> paths, Optional<Set<String>> methods, RateLimitPhase phase,
        String key, String store, Optional<String> limitResolver, Optional<RateLimit> limit, Optional<RateLimit> fallback,
        long maxKeys) {

    /**
     * Only for configuration that passed {@link HttpRateLimitConfigValidator#validate}.
     */
    public static List<RateLimitRuleConfig> all(HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig runtimeConfig) {
        List<RateLimitRuleConfig> rules = new ArrayList<>();
        for (String name : buildConfig.rules().keySet()) {
            if (runtimeConfig.rules().get(name).enabled()) {
                rules.add(of(name, buildConfig, runtimeConfig));
            }
        }
        return rules;
    }

    public static RateLimitRuleConfig of(String name, HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig runtimeConfig) {
        HttpRateLimitBuildTimeConfig.Rule build = buildConfig.rules().get(name);
        HttpRateLimitRuntimeConfig.Rule runtime = runtimeConfig.rules().get(name);
        Optional<RateLimit> limit = runtime.limit().map(n -> named(runtimeConfig, n));
        if (runtime.permits().isPresent()) {
            limit = Optional.of(RateLimit.of(runtime.permits().get(), runtime.window().orElseThrow(),
                    runtime.burst().orElse(runtime.permits().get())));
        }
        return new RateLimitRuleConfig(name, build.paths(), build.methods(),
                phase(buildConfig, build), key(buildConfig, build), store(buildConfig, build),
                limitResolver(buildConfig, build, runtime), limit,
                runtime.fallbackLimit().map(n -> named(runtimeConfig, n)),
                runtime.maxKeys().orElse(runtimeConfig.maxKeys()));
    }

    public static RateLimit named(HttpRateLimitRuntimeConfig config, String name) {
        HttpRateLimitRuntimeConfig.NamedLimit limit = config.limits().get(name);
        if (limit == null) {
            throw new IllegalArgumentException("Unknown rate limit '" + name
                    + "': configure quarkus.http.rate-limit.limits.\"" + name + "\".permits and window");
        }
        return RateLimit.of(limit.permits(), limit.window(), limit.burst().orElse(limit.permits()));
    }

    /**
     * @return the rule phase, else the shared phase; null when neither is set, which fails the build
     */
    public static RateLimitPhase phase(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.phase().orElse(config.phase().orElse(null));
    }

    /**
     * @return the rule key, else the shared key; null when neither is set, which fails the build
     */
    public static String key(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.requestKey().orElse(config.requestKey().orElse(null));
    }

    /**
     * A static rule opts out of an inherited resolver. An explicitly selected resolver cannot coexist with a static limit.
     */
    public static Optional<String> limitResolver(HttpRateLimitBuildTimeConfig config,
            HttpRateLimitBuildTimeConfig.Rule rule, HttpRateLimitRuntimeConfig.Rule runtime) {
        if (rule.limitResolver().isPresent()) {
            return rule.limitResolver();
        }
        if (runtime.limit().isPresent() || runtime.permits().isPresent() || runtime.window().isPresent()) {
            return Optional.empty();
        }
        return config.limitResolver();
    }

    public static String store(HttpRateLimitBuildTimeConfig config, HttpRateLimitBuildTimeConfig.Rule rule) {
        return rule.store().orElse(config.store());
    }

    public String id() {
        return name + "/" + phase.configName();
    }
}

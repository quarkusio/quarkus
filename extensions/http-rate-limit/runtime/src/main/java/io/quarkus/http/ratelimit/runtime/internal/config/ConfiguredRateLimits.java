package io.quarkus.http.ratelimit.runtime.internal.config;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimits;

/**
 * Immutable registry populated from runtime configuration.
 */
@Singleton
public class ConfiguredRateLimits implements RateLimits {
    private final Map<String, RateLimit> limits;

    @Inject
    public ConfiguredRateLimits(HttpRateLimitRuntimeConfig config) {
        Map<String, RateLimit> values = new HashMap<>();
        for (String name : config.limits().keySet()) {
            values.put(name, RateLimitRuleConfig.named(config, name));
        }
        limits = Map.copyOf(values);
    }

    @Override
    public RateLimit get(String name) {
        RateLimit limit = limits.get(name);
        if (limit == null) {
            throw new IllegalArgumentException("Unknown rate limit '" + name + "'");
        }
        return limit;
    }
}

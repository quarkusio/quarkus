package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Map;

import jakarta.enterprise.inject.Instance;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;

/**
 * Maps the {@code request-key} of a rule to its resolver.
 */
public final class KeyResolvers {

    private static final String HEADER_KEY_PREFIX = "header:";
    private static final Map<String, Class<? extends RateLimitKeyResolver>> BUILT_IN_KEYS = Map.of(
            "ip", IpKeyResolver.class,
            "global", GlobalKeyResolver.class,
            "principal", PrincipalKeyResolver.class);

    private KeyResolvers() {
    }

    /**
     * @param key a built-in key or {@code header:<name>}, which the build has validated
     * @param resolvers the built-in {@link RateLimitKeyResolver} beans
     */
    public static RateLimitKeyResolver forKey(String key, Instance<RateLimitKeyResolver> resolvers) {
        if (key.startsWith(HEADER_KEY_PREFIX)) {
            return new HeaderKeyResolver(key.substring(HEADER_KEY_PREFIX.length()));
        }
        return resolvers.select(BUILT_IN_KEYS.get(key)).get();
    }
}

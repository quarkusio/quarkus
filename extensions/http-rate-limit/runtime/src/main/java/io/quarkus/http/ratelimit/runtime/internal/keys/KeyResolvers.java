package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Map;

import jakarta.enterprise.inject.Instance;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.smallrye.common.annotation.Identifier;

/**
 * Maps the {@code request-key} of a rule to its resolver.
 */
public final class KeyResolvers {

    private static final String HEADER_KEY_PREFIX = "header:";
    private static final String CUSTOM_KEY_PREFIX = "custom:";
    private static final Map<String, Class<? extends RateLimitKeyResolver>> BUILT_IN_KEYS = Map.of(
            "ip", IpKeyResolver.class,
            "path", PathKeyResolver.class,
            "global", GlobalKeyResolver.class,
            "principal", PrincipalKeyResolver.class);

    private KeyResolvers() {
    }

    /**
     * @param key a built-in key, {@code header:<name>}, or {@code custom:} and the {@code @Identifier} of a
     *        {@link RateLimitKeyResolver} bean
     * @param resolvers every {@link RateLimitKeyResolver} bean, built-in ones included
     */
    public static RateLimitKeyResolver forKey(String key, Instance<RateLimitKeyResolver> resolvers) {
        Class<? extends RateLimitKeyResolver> builtIn = BUILT_IN_KEYS.get(key);
        if (builtIn != null) {
            return resolvers.select(builtIn).get();
        }
        if (key.startsWith(HEADER_KEY_PREFIX) && key.length() > HEADER_KEY_PREFIX.length()) {
            return new HeaderKeyResolver(key.substring(HEADER_KEY_PREFIX.length()));
        }
        if (key.startsWith(CUSTOM_KEY_PREFIX) && key.length() > CUSTOM_KEY_PREFIX.length()) {
            return resolvers.select(Identifier.Literal.of(key.substring(CUSTOM_KEY_PREFIX.length()))).get();
        }
        // validated at build time
        throw new IllegalArgumentException("Unknown request-key '" + key + "'");
    }
}

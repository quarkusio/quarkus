package io.quarkus.oidc.client.reactive.filter.runtime;

import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class OidcClientReactiveFilterConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.oidc-client-reactive-filter.";
    private static final String NEW_PREFIX = "quarkus.rest-client-oidc-filter.";

    public OidcClientReactiveFilterConfigFallbackInterceptor() {
        super(OidcClientReactiveFilterConfigFallbackInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(NEW_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(NEW_PREFIX, OLD_PREFIX);
    }
}

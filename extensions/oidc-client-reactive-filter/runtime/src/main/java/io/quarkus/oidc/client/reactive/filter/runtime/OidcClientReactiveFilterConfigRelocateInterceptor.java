package io.quarkus.oidc.client.reactive.filter.runtime;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class OidcClientReactiveFilterConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.oidc-client-reactive-filter.";
    private static final String NEW_PREFIX = "quarkus.rest-client-oidc-filter.";

    public OidcClientReactiveFilterConfigRelocateInterceptor() {
        super(OidcClientReactiveFilterConfigRelocateInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(OLD_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(OLD_PREFIX, NEW_PREFIX);
    }
}

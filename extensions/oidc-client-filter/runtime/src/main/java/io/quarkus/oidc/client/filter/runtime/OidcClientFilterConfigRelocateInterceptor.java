package io.quarkus.oidc.client.filter.runtime;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class OidcClientFilterConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.oidc-client-filter.";
    private static final String NEW_PREFIX = "quarkus.resteasy-client-oidc-filter.";

    public OidcClientFilterConfigRelocateInterceptor() {
        super(OidcClientFilterConfigRelocateInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(OLD_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(OLD_PREFIX, NEW_PREFIX);
    }
}

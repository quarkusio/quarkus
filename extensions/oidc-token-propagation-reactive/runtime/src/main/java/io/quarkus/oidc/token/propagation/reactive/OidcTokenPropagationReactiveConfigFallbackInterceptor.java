package io.quarkus.oidc.token.propagation.reactive;

import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class OidcTokenPropagationReactiveConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.oidc-token-propagation.";
    private static final String NEW_PREFIX = "quarkus.rest-client-oidc-token-propagation.";

    public OidcTokenPropagationReactiveConfigFallbackInterceptor() {
        super(OidcTokenPropagationReactiveConfigFallbackInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(NEW_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(NEW_PREFIX, OLD_PREFIX);
    }
}

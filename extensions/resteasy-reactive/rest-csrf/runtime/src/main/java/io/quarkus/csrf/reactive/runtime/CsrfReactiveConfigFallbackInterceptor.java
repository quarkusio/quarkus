package io.quarkus.csrf.reactive.runtime;

import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class CsrfReactiveConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.csrf-reactive.";
    private static final String NEW_PREFIX = "quarkus.rest-csrf.";

    public CsrfReactiveConfigFallbackInterceptor() {
        super(CsrfReactiveConfigFallbackInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(NEW_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(NEW_PREFIX, OLD_PREFIX);
    }
}

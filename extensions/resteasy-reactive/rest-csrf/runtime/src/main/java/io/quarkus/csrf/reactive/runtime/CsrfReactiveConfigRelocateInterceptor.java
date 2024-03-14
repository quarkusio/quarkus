package io.quarkus.csrf.reactive.runtime;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class CsrfReactiveConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.csrf-reactive.";
    private static final String NEW_PREFIX = "quarkus.rest-csrf.";

    public CsrfReactiveConfigRelocateInterceptor() {
        super(CsrfReactiveConfigRelocateInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(OLD_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(OLD_PREFIX, NEW_PREFIX);
    }
}

package io.quarkus.resteasy.reactive.common.runtime;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class ResteasyReactiveConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.resteasy-reactive.";
    private static final String NEW_PREFIX = "quarkus.rest.";

    public ResteasyReactiveConfigRelocateInterceptor() {
        super(ResteasyReactiveConfigRelocateInterceptor::rename);
    }

    private static String rename(String originalName) {
        if (!originalName.startsWith(OLD_PREFIX)) {
            return originalName;
        }

        return originalName.replaceFirst(OLD_PREFIX, NEW_PREFIX);
    }
}

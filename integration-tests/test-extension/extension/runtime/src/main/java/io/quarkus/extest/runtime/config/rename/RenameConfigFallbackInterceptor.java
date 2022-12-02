package io.quarkus.extest.runtime.config.rename;

import java.util.function.Function;

import io.smallrye.config.FallbackConfigSourceInterceptor;

public class RenameConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {
    private static final Function<String, String> FALLBACK = name -> {
        if (name.startsWith("quarkus.rename.")) {
            return name.replaceFirst("quarkus\\.rename\\.", "quarkus.rename-old.");
        }
        return name;
    };

    public RenameConfigFallbackInterceptor() {
        super(FALLBACK);
    }
}

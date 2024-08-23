package io.quarkus.extest.runtime.config.rename;

import java.util.function.Function;

import io.smallrye.config.RelocateConfigSourceInterceptor;

public class RenameConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {
    private static final Function<String, String> RELOCATE = name -> {
        if (name.startsWith("quarkus.rename-old.")) {
            return name.replaceFirst("quarkus\\.rename-old\\.", "quarkus.rename.");
        }
        return name;
    };

    public RenameConfigRelocateInterceptor() {
        super(RELOCATE);
    }
}

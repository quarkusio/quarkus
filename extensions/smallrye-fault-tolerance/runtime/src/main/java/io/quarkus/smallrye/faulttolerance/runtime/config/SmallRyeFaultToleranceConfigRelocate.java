package io.quarkus.smallrye.faulttolerance.runtime.config;

import java.util.function.Function;

import io.smallrye.config.RelocateConfigSourceInterceptor;

public class SmallRyeFaultToleranceConfigRelocate extends RelocateConfigSourceInterceptor {
    private static final Function<String, String> RELOCATION = name -> {
        if (name.startsWith("smallrye.faulttolerance.")) {
            return name.replaceFirst("smallrye\\.faulttolerance\\.", "quarkus.fault-tolerance.");
        }
        return name;
    };

    public SmallRyeFaultToleranceConfigRelocate() {
        super(RELOCATION);
    }
}

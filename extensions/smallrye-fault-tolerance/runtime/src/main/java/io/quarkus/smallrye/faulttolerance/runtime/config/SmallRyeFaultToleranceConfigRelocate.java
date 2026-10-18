package io.quarkus.smallrye.faulttolerance.runtime.config;

import java.util.OptionalInt;
import java.util.function.Function;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;

@StaticInitSafe
public class SmallRyeFaultToleranceConfigRelocate implements ConfigSourceInterceptorFactory {
    private static final Function<String, String> RELOCATION = name -> {
        if (name.startsWith("smallrye.faulttolerance.")) {
            return name.replaceFirst("smallrye\\.faulttolerance\\.", "quarkus.fault-tolerance.");
        }
        return name;
    };

    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
        return new RelocateConfigSourceInterceptor(RELOCATION);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(Priorities.LIBRARY + 300);
    }
}

package io.quarkus.quartz.runtime;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuartzRecorder {

    public Supplier<QuartzSupport> quartzSupportSupplier(QuartzRuntimeConfig runtimeConfig,
            QuartzBuildTimeConfig buildTimeConfig, Optional<String> driverDialect, Set<String> nonconcurrentMethods) {
        return new Supplier<QuartzSupport>() {
            @Override
            public QuartzSupport get() {
                return new QuartzSupport(runtimeConfig, buildTimeConfig, driverDialect, nonconcurrentMethods);
            }
        };
    }

}

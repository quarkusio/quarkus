package io.quarkus.quartz.runtime;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuartzRecorder {
    private final QuartzBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<QuartzRuntimeConfig> runtimeConfig;

    public QuartzRecorder(
            final QuartzBuildTimeConfig buildTimeConfig,
            final RuntimeValue<QuartzRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public Supplier<QuartzSupport> quartzSupportSupplier(Optional<String> driverDialect, Set<String> nonconcurrentMethods) {
        return new Supplier<QuartzSupport>() {
            @Override
            public QuartzSupport get() {
                return new QuartzSupport(runtimeConfig.getValue(), buildTimeConfig, driverDialect, nonconcurrentMethods);
            }
        };
    }

}

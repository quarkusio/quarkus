package io.quarkus.quartz.runtime;

import java.util.Optional;
import java.util.Set;

import io.quarkus.quartz.Nonconcurrent;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;

public class QuartzSupport {

    private final QuartzRuntimeConfig runtimeConfig;
    private final QuartzBuildTimeConfig buildTimeConfig;
    private final Optional<String> driverDialect;
    // <FQCN>#<method_name>
    private final Set<String> nonconcurrentMethods;

    public QuartzSupport(QuartzRuntimeConfig runtimeConfig, QuartzBuildTimeConfig buildTimeConfig,
            Optional<String> driverDialect, Set<String> nonconcurrentMethods) {
        this.runtimeConfig = runtimeConfig;
        this.buildTimeConfig = buildTimeConfig;
        this.driverDialect = driverDialect;
        this.nonconcurrentMethods = Set.copyOf(nonconcurrentMethods);
    }

    public QuartzRuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public QuartzBuildTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public Optional<String> getDriverDialect() {
        return driverDialect;
    }

    /**
     *
     * @param method
     * @return {@code true} if the scheduled method is annotated with {@link Nonconcurrent}
     */
    public boolean isNonconcurrent(ScheduledMethod method) {
        return nonconcurrentMethods.contains(method.getMethodDescription());
    }

}

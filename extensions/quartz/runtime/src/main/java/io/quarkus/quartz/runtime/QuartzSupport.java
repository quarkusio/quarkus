package io.quarkus.quartz.runtime;

import java.util.Optional;

public class QuartzSupport {

    private final QuartzRuntimeConfig runtimeConfig;
    private final QuartzBuildTimeConfig buildTimeConfig;
    private final Optional<String> driverDialect;

    public QuartzSupport(QuartzRuntimeConfig runtimeConfig, QuartzBuildTimeConfig buildTimeConfig,
            Optional<String> driverDialect) {
        this.runtimeConfig = runtimeConfig;
        this.buildTimeConfig = buildTimeConfig;
        this.driverDialect = driverDialect;
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
}

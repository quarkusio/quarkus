package io.quarkus.quartz.runtime;

import java.util.Optional;

import javax.inject.Singleton;

@Singleton
public class QuartzSupport {

    private QuartzRuntimeConfig runtimeConfig;
    private QuartzBuildTimeConfig buildTimeConfig;
    private Optional<String> driverDialect;

    void initialize(QuartzRuntimeConfig runTimeConfig, QuartzBuildTimeConfig buildTimeConfig, Optional<String> driverDialect) {
        this.runtimeConfig = runTimeConfig;
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

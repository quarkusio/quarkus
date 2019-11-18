package io.quarkus.quartz.runtime;

import javax.inject.Singleton;

@Singleton
public class QuartzSupport {

    private QuartzRuntimeConfig runtimeConfig;

    void initialize(QuartzRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public QuartzRuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

}

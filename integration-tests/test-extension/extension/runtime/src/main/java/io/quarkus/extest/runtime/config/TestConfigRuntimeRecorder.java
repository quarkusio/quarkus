package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TestConfigRuntimeRecorder {
    private final TestMappingRunTime runtimeConfig;

    public TestConfigRuntimeRecorder(TestMappingRunTime runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public TestMappingRunTime getRuntimeConfig() {
        return runtimeConfig;
    }
}

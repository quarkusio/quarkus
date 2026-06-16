package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TestConfigBuildTimeRecorder {
    private TestMappingBuildTime buildTime;

    public TestConfigBuildTimeRecorder(TestMappingBuildTime buildTime) {
        this.buildTime = buildTime;
    }

    public TestMappingBuildTime getBuildTime() {
        return buildTime;
    }
}
